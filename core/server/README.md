# core:server · Ktor 服务与前台服务

内嵌 HTTP / WebSocket 服务、设备在线推导、上传任务跟踪、服务状态机与前台服务。

## 结构

| 类型 | 职责 |
| --- | --- |
| `http/ShareServer` | Ktor CIO 服务、路由、静态资源、WebSocket、错误处理 |
| `http/ApiDtos` | `@Serializable` 请求 / 响应 DTO |
| `DeviceRegistry` | 访问设备活动表、在线判定、上下行流量统计 |
| `UploadMonitor` | 上传任务进度与完成 / 失败事件 |
| `ServerEventBus` | 文件变更、设备变更、上传事件广播 |
| `ShareController` | 启停状态机、运行指标、对外暴露 StateFlow |
| `service/ShareService` | 前台服务与常驻通知 |
| `service/BootReceiver` | 开机广播按设置自启 |
| `di/ServerModule` | Koin 装配 |

## API 契约（`/api/v1`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/server-info` | `{ server_url, ip, port, version, allow_upload }` |
| GET | `/files?q=&page=&size=` | `{ files: [{ name, size, modified }], total, page, size }`，`modified` 为 Unix 秒 |
| POST | `/upload` | multipart 字段 `file`，流式写盘；超出上限 413，关闭上传 403 |
| GET | `/download/{filename}` | 下载；`Content-Disposition` 按 RFC 5987 编码中文名 |
| DELETE | `/files/{filename}` | 删除单个文件 |
| DELETE | `/files` | 清空全部，返回 `{ count }` |
| GET | `/devices` | 在线设备列表 |
| WS | `/ws` | 浏览器心跳（发送 `ping`）与服务端事件推送 |
| GET | `/static/*` | `assets/web` 中的浏览器页面资源 |

错误统一为 `{ "detail": "错误信息" }`；文件名拒绝 `/`、`\`、`..` 与隐藏文件。

## 行为细节

- **流式传输**：上传边读边写，进度回调按 150ms 节流；下载按 64KB 分块复制并累计流量。
- **上传开关**：每次请求读取最新设置；`server-info` 同步返回 `allow_upload`，网页端据此隐藏上传区。
- **设备在线**：HTTP 请求刷新 `lastSeen`，WebSocket 连接额外计数；30 秒无活动视为离线，5 分钟无连接记录被清理。设备类型由 UA 推断，`localhost` / IPv6 回环统一显示为 `127.0.0.1`。
- **运行指标**：1 秒定时器计算运行时长、上下行速率与累计传输量。
- **状态机**：`Stopped → Starting → Running`，绑定失败进入 `Failed`；启动超时 5 秒，端口占用会提示更换端口。
- **前台服务**：启动即 `startForeground`，通知文案随状态更新（地址 / 启动失败 / 停止中），通知自带「停止服务」动作；`START_STICKY`，失败状态 2 秒后停止服务且不会误停重新启动的实例。
- **开机自启**：`BootReceiver` 读取 `autostart` 后启动前台服务，异常静默处理。

## 调试

```bash
# 启动服务后（模拟器）
adb forward tcp:8080 tcp:8080
curl http://127.0.0.1:8080/api/v1/server-info
curl -F "file=@demo.txt" http://127.0.0.1:8080/api/v1/upload
curl "http://127.0.0.1:8080/api/v1/files"
```

## 安全边界（v1）

服务监听 `0.0.0.0`，同一局域网内无需认证即可访问；仅在可信网络中开启，App 内已有提示。路径穿越与非法文件名在服务端拦截，静态资源只读取 `assets/web` 下的文件。
