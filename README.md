# FileShare · 局域网文件共享 App

基于 **Kotlin + Jetpack Compose + Ktor** 的 Android 局域网文件共享应用。手机一键开启内嵌 HTTP 服务，同一局域网内的任何设备用浏览器访问即可上传、下载、删除文件，全程不经过任何第三方服务器。

> 设计文档：[功能清单](docs/局域网文件共享App功能清单-v1.md) · [功能设计](docs/局域网文件共享App功能设计-v1.md) · [交互原型](docs/局域网文件共享App交互原型-v1.html)

## 功能总览

| 模块 | 能力 |
|------|------|
| 服务控制 | 一键启停（大号电源按钮 + 呼吸灯）、前台服务常驻通知、开机自启、屏幕常亮、电池优化引导 |
| 局域网访问 | 自动识别局域网 IP、一键复制地址、二维码扫码访问、已连接设备实时列表（UA 解析设备类型） |
| 文件管理 | 浏览器端拖拽/多选上传（流式写盘 + 进度）、文件列表、搜索（150ms 防抖）、分页（8/页）、下载直链复制、删除/清空 |
| 传输配置 | 端口（8080/8000/9090）、单文件大小上限、允许上传开关、SAF 自定义保存目录 |
| 系统适配 | SAF 持久化授权、所有文件访问权限、忽略电池优化、Android 13+ 通知权限 |

浏览器端页面（`app/src/main/assets/web/`）沿用 FileShareTs 的静态页面，接口契约保持一致。

## 技术栈

- **Kotlin 2.3.21** / **Jetpack Compose**（Material 3，Compose BOM 2026.11.01）
- **Ktor Server 3.6.2**（CIO 引擎、WebSocket、ContentNegotiation、StatusPages）
- **Koin 4.2.2** 依赖注入（Compose / ViewModel 集成）
- **DataStore Preferences** 设置存储，**SAF / File API** 双实现文件存储
- **ZXing** 二维码生成，**kotlinx.serialization** JSON
- 多模块 + **build-logic 约定插件**，版本统一由 `gradle/libs.versions.toml` 管理

## 模块结构

```
app                      # Application、MainActivity、底部导航、主题入口、Manifest
core/
  model                  # 纯 Kotlin 领域模型（设置、服务状态、设备、上传任务、事件）
  common                 # 格式化、文件名安全、UA 解析、局域网 IP、文件类型
  designsystem           # 设计令牌与通用 Compose 组件
  data                   # DataStore 设置仓库 + 文件存储（File / SAF）
  server                 # Ktor HTTP/WS 服务、设备登记、上传监控、服务控制器、前台服务、开机广播
feature/
  share                  # 服务页：电源、地址、二维码、设备列表、运行指标
  files                  # 文件页：搜索、分页、上传进度、导入、复制直链、删除/清空
  settings               # 设置页：端口、限额、开关、保存目录、系统权限引导
build-logic/             # Gradle 约定插件（application / library / compose / feature）
```

依赖方向：`feature → core:*`，`core:server → core:data → core:common → core:model`，`app → feature:*`。

## 构建与运行

环境要求：JDK 17+、Android SDK Platform 36（`compileSdk = 36`，`minSdk = 26`）。

```powershell
.\gradlew :app:assembleDebug
```

```powershell
.\gradlew :app:installDebug
```

仓库与 Gradle 分发包均已配置国内镜像：

- Maven：阿里云 `google` / `public` / `gradle-plugin` 仓库优先，官方仓库兜底（`settings.gradle.kts`）
- Gradle 8.14.3：腾讯云镜像（`gradle/wrapper/gradle-wrapper.properties`）
  - 若下载异常，可换成华为云：`https://mirrors.huaweicloud.com/gradle/gradle-8.14.3-bin.zip`

如果构建时未授予「所有文件访问」，文件会自动保存到应用专属目录（`Android/data/com.fileshare.lan/files/FileShare`）；在设置页授权后可写入 `Download/FileShare`，也可通过系统文件夹选择器指定任意目录。

## API 契约（`/api/v1`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/server-info` | `{ server_url, ip, port, version }` |
| GET | `/files?q=&page=&size=` | `{ files: [{ name, size, modified }], total, page, size }`（`modified` 为 Unix 秒） |
| POST | `/upload` | multipart 字段 `file`，流式写盘，超出限额返回 413 |
| GET | `/download/{filename}` | 下载，`Content-Disposition` 按 RFC 5987 编码中文名 |
| DELETE | `/files/{filename}` | 删除单个文件 |
| DELETE | `/files` | 清空全部文件，返回 `{ count }` |
| GET | `/devices` | 在线设备列表 |
| WS | `/ws` | 浏览器心跳（发送 `ping`）与服务端事件推送 |
| GET | `/static/*` | 浏览器端静态资源（`assets/web`） |

错误统一返回 `{ "detail": "错误信息" }`；文件名拒绝路径穿越（`/`、`\`、`..`、隐藏文件）。

## 说明与限制（v1）

- 仅共享单层目录，不做用户认证（同一局域网内均可访问，App 内已明示）。
- 服务端不持久化传输历史；进程被杀后未完成的上传临时文件会被清理。
- Android 15+ 对 `BOOT_COMPLETED` 启动 `dataSync` 前台服务有限制，开机自启在部分 ROM 上可能被系统拦截，需在系统设置中允许自启动。
- 使用前台服务请勿从最近任务划掉 App（force-stop 后系统不再派发开机广播）。
