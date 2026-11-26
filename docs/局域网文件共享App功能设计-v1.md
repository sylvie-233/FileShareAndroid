# 局域网文件共享 App · 功能设计（v1）

> 交互原型：[局域网文件共享App交互原型-v1.html](./局域网文件共享App交互原型-v1.html)
> 功能清单：[局域网文件共享App功能清单-v1.md](./局域网文件共享App功能清单-v1.md)
> 功能参考：FileShareTs（Hono + TypeScript），API 契约尽量沿用，便于复用其浏览器端页面

---

## 1. 总体架构

```
┌─────────────────────────── Android App ───────────────────────────┐
│                                                                    │
│  UI 层（原型三 Tab）          服务层                    数据层        │
│  ┌──────────────┐   ┌───────────────────────┐   ┌──────────────┐  │
│  │ 服务 Tab      │──▶│ ShareService（前台服务）│──▶│ 共享目录      │  │
│  │  电源开关     │   │  · 内嵌 HTTP Server    │   │ (SAF/File)   │  │
│  │  连接设备     │   │  · 路由 /api/v1/*      │   └──────────────┘  │
│  │ 文件 Tab      │──▶│  · 设备活动表           │   ┌──────────────┐  │
│  │  列表/搜索    │   └───────────┬───────────┘   │ SettingsStore │  │
│  │ 设置 Tab      │               │ 事件广播       │ (端口/限额/目录)│  │
│  └──────────────┘               ▼               └──────────────┘  │
│                      App 内 UI 实时刷新（LocalBroadcast/Flow）      │
└────────────────────────────────────────────────────────────────────┘
                 ▲                       ▲
          局域网设备 A                局域网设备 B
        （浏览器：上传/下载）       （浏览器：上传/下载）
```

技术选型建议：

| 模块 | 建议 | 备选 |
|------|------|------|
| 内嵌 HTTP 服务 | Ktor（协程友好、WebSocket 原生支持） | NanoHTTPD（更轻，无 WebSocket） |
| UI | 原生（Jetpack Compose）或 WebView 复用原型 | Flutter |
| 文件写入 | `ContentResolver.openOutputStream()`（SAF）或 `File`（全权限） | — |
| 状态存储 | SharedPreferences / DataStore | — |
| 服务间通信 | StateFlow / LocalBroadcastManager | EventBus |

## 2. 服务生命周期设计

### 2.1 状态机

```
STOPPED ──点击电源/开机广播──▶ STARTING ──端口绑定成功──▶ RUNNING
   ▲                            │                           │
   │                            └──绑定失败──▶ STOPPED（toast 报错）
   └────────关闭确认/异常退出────────────────────────────────┘
```

- **STARTING**：获取局域网 IP → 绑定端口 → 启动 WebSocket 端点 → 切入前台服务通知。超时 3s 未成功则报错回退（常见原因：端口被占用，提示换端口）。
- **RUNNING**：心跳指标定时器启动；请求活动表开始记录；收到上传/下载事件后广播给 UI 刷新。
- **STOPPED**：释放端口、关闭所有 WebSocket 连接、停止计时器、移除常驻通知。

### 2.2 前台服务

- `ShareService` 为前台服务（`foregroundServiceType="dataSync"`），启动即发常驻通知：「共享服务运行中 · http://192.168.x.x:8080」。
- App 进程被杀不影响服务（服务独立于 Activity 生命周期）；用户从最近任务划掉 App 属 force-stop，系统不再派发开机广播，需在 UI 提示用户不要划掉。

### 2.3 开机自启

- `BootReceiver` 监听 `BOOT_COMPLETED`，读取设置项 `autostart`，为 true 时 `startForegroundService()`。
- 国产 ROM（MIUI/鸿蒙/ColorOS）需检测自启动权限未授权时展示引导页。
- 电池优化：申请 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 或引导用户手动加白。

## 3. API 设计（沿用 FileShareTs 契约）

所有接口位于 `/api/v1`，错误响应统一 `{ "detail": "错误信息" }`：

| 方法 | 路径 | 说明 | 增改点 |
|------|------|------|--------|
| GET | `/api/v1/server-info` | 服务端信息（IP、端口、版本） | 沿用 |
| POST | `/api/v1/upload` | 上传文件（multipart，字段名 `file`） | 增加流式写盘 + 上传进度事件 |
| GET | `/api/v1/files` | 文件列表（支持 `?q=搜索&page=1&size=20`） | 沿用 |
| GET | `/api/v1/download/{filename}` | 下载文件，UTF-8 文件名 | 沿用 |
| DELETE | `/api/v1/files/{filename}` | 删除单个文件 | 沿用 |
| DELETE | `/api/v1/files` | 清空全部文件 | 沿用 |
| GET | `/api/v1/devices` | 在线设备列表 | **新增** |
| WS | `/api/v1/ws` | 浏览器心跳 + 服务端推送（新文件、设备上线/离线） | **新增** |
| GET | `/static/*` | 浏览器端静态资源（index.html / qrcode.js / favicon.ico） | 沿用 |

### 访问端 Web 页面（已就绪）

浏览器端页面（即"开启服务后其他设备看到的网页"）**已从 FileShareTs 迁入本项目**，位于：

```
docs/assets/
├── index.html    # 浏览器端页面：拖拽/多选上传、文件列表、搜索分页、二维码
├── qrcode.js     # 二维码生成库
└── favicon.ico   # 站点图标
```

Android 集成方式：将 `docs/assets/` 内容放入 App 的 **`src/main/assets/web/`**，内嵌 HTTP 服务将 `/static/*` 路由映射到 assets：

```kotlin
// Ktor：从 assets 读取静态文件
staticResources("/static", "web")
// 或 NanoHTTPD：读 assets 流返回，Content-Type 按扩展名映射
```

页面要求服务在 `/api/v1/*` 上提供与前述契约一致的接口即可正常运行，无需修改页面代码。

### 关键行为约定

- **流式传输**：上传用 multipart 流式解析边收边写，不整读入内存；下载直接 `sendFile`/`transferFrom`。
- **上传限制**：超出 `maxFileSize` 返回 413 + `{"detail": "文件超过大小限制"}`；`allowUpload=false` 时 POST /upload 返回 403。
- **中文文件名**：URL 解码按 UTF-8；下载响应头 `Content-Disposition` 按 RFC 5987 编码（`filename*=UTF-8''...`）。
- **安全边界**：文件名拒绝路径穿越（禁止 `/`、`\`、`..`）；服务只监听 `0.0.0.0` 的局域网可达地址，v1 不做鉴权（同一局域网内均可访问，需在 App 内明示）。

## 4. 已连接设备检测设计

HTTP 无状态，"在线"是推导出来的，采用**两层机制**：

### 4.1 请求活动表（兜底）

```
Map<ip, DeviceInfo{ userAgent, lastSeenAt, uploadBytes, downloadBytes }>
```

- 每个 HTTP 请求经拦截器刷新 `lastSeenAt`；
- 超过 **30s** 无新请求标记离线；
- 设备类型从 User-Agent 解析：Android 机型型号 / iPhone / iPad / Windows / Mac / 未知。

### 4.2 WebSocket 心跳（主通道）

- 浏览器页面打开即连 `/api/v1/ws`，每 10s 发 ping；
- 连接建立 → 加入在线集合；断开 → 移出；
- 服务端通过同一通道推送事件：`{"event":"file-added", ...}`、`{"event":"device-online", ...}`，浏览器端列表实时刷新。

### 4.3 显示策略

- 列表项：`IP 地址 + 设备类型 + 在线/离线标签`；
- v1 **不显示真实设备名**（浏览器不提供），如需昵称，可在浏览器页面加"设备昵称"输入（localStorage 保存后随心跳上报），此为 v2 可选项。

## 5. 存储设计

### 5.1 保存目录

- 默认：`Download/FileShare`；
- 修改：SAF `ACTION_OPEN_DOCUMENT_TREE` 系统文件夹选择器，`takePersistableUriPermission` 持久化授权，URI 存入 SettingsStore；
- 若申请了"所有文件访问"权限（侧载场景推荐），则直接用 `File` API，路径性能最优；
- 重命名冲突：同名文件自动追加 `(1)`、`(2)` 后缀。

### 5.2 设置项

| Key | 类型 | 默认 | 说明 |
|-----|------|------|------|
| `port` | Int | 8080 | 重启生效 |
| `autostart` | Boolean | true | 开机自启 |
| `keepAwake` | Boolean | true | 屏幕常亮 |
| `maxFileSize` | Long | 100MB | 0 表示不限 |
| `allowUpload` | Boolean | true | 允许浏览器端上传 |
| `saveDirUri` | String | 默认目录 URI | SAF tree URI |

## 6. UI 与交互设计（对应原型）

- **三个 Tab**：服务 / 文件 / 设置；底部导航常驻；
- **电源按钮**：STOPPED 灰色、RUNNING 绿色呼吸灯、STARTING 旋转动画且不可点；
- **危险操作**（删除/清空/关服务）：统一确认弹窗，明确文案 + 红色确认按钮；
- **实时反馈**：Toast 提示操作结果；上传任务在文件页顶部显示进度条；
- **实时刷新**：服务层通过 Flow/LiveData 向 UI 推送文件新增、设备上下线、传输速率（1s 节流）。

## 7. 错误与边界场景

| 场景 | 处理 |
|------|------|
| 端口被占用 | STARTING 失败 → 提示"端口被占用，请更换端口" |
| 无 Wi-Fi / 热点未开 | 启动时检测无局域网 IP → 提示"请先连接 Wi-Fi 或开启热点" |
| 传输中服务被杀 | 已收完的文件保留；未完成文件删除临时文件 |
| 磁盘空间不足 | 上传前预估，不足返回 507 + 提示 |
| 同名冲突 | 自动重命名 `(1)` |
| 路径穿越攻击 | 文件名校验，拒绝 `/`、`..` 等 |
| 手机热点模式 | 支持（热点网段同样是局域网），地址随热点网段变化 |

## 8. 版本规划

- **v1（当前）**：单目录共享、HTTP 上传下载、设备在线检测、开机自启、SAF 目录
- **v2（候选）**：访问密码、子目录浏览、断点续传、深色模式、传输历史
- **v3（候选）**：App 直传（mDNS 发现 + TCP 直连）、多设备同步、文件预览
