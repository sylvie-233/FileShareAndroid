# core:model · 领域模型

纯 Kotlin JVM 模块，不依赖 Android，作为 UI、数据层与服务层共享的数据契约。

## 模型一览

| 类型 | 说明 |
| --- | --- |
| `SharedFile` | 共享目录中的文件：`name`、`sizeBytes`、`modifiedAtMillis` |
| `AppSettings` | 应用设置：端口、开机自启、屏幕常亮、单文件上限、允许上传、SAF 目录 URI |
| `DeviceType` | 由 User-Agent 推断的设备类型：Android / iPhone / iPad / Windows / Mac / 其他 |
| `DeviceInfo` | 访问设备：IP、UA、设备类型、最后活跃时间、在线状态、上传 / 下载字节数 |
| `ServerState` | 服务状态机：`Stopped` / `Starting` / `Stopping` / `Running(ServerInfo)` / `Failed(message)` |
| `ServerInfo` | 运行信息：访问 URL、IP、端口 |
| `ServerMetrics` | 运行指标：运行时长、上行 / 下行速率、累计传输字节 |
| `UploadTask` / `UploadStatus` | 上传任务：文件名、总大小、已接收字节、进度、状态 |
| `ServerEvent` | 服务端事件：文件变更、设备变更、上传任务更新 |

## 默认值

`AppSettings` 默认端口 `8080`、单文件上限 `100 MB`，并提供：

- `PORT_OPTIONS = [8080, 8000, 9090]`
- `MAX_FILE_SIZE_OPTIONS = [100MB, 1GB, 0]`（0 表示不限）

API 的序列化 DTO 位于 `core:server` 的 `http/ApiDtos.kt`，与浏览器页面契约保持一致；领域模型本身不参与 JSON 序列化。
