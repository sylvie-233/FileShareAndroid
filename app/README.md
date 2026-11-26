# app · 应用壳

应用入口与集成层：启动 Koin、三 Tab 导航、全局状态徽章、权限与前台服务声明，并把浏览器静态资源打包进 APK。

## 目录

```text
app/src/main/
├─ kotlin/com/fileshare/lan/
│  ├─ FileShareApplication.kt   Koin 启动与模块装配
│  ├─ MainActivity.kt           边到边、通知权限、屏幕常亮、Compose 入口
│  └─ ui/FileShareApp.kt        Scaffold + 顶部品牌 / 状态徽章 + 底部导航
├─ res/                         主题、启动图标（前景 / 渐变背景 / 单色层）
└─ assets/web/                  浏览器端页面（index.html、qrcode.js、favicon.ico）
```

## 启动链路

1. `FileShareApplication` 调用 `startKoin`，装配 `dataModule`、`serverModule`、`shareModule`、`filesModule`、`settingsModule`。
2. `MainActivity` 调用 `enableEdgeToEdge()`，并把 `FileShareTheme` + `FileShareApp` 设为内容；Android 13+ 首次启动请求通知权限。
3. `FileShareApp` 通过 `ShareController.state` 展示全局运行徽章，底部三个 Tab 分别渲染 `ShareScreen` / `FilesScreen` / `SettingsScreen`。
4. 服务运行且设置中开启「保持屏幕常亮」时，Activity 给窗口加 `FLAG_KEEP_SCREEN_ON`。

## 权限

| 权限 | 用途 |
| --- | --- |
| `INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE` | 内嵌服务与局域网 IP 识别 |
| `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_DATA_SYNC` | 共享服务常驻前台 |
| `POST_NOTIFICATIONS` | Android 13+ 前台服务通知 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `WAKE_LOCK` | 屏幕常亮 |
| `MANAGE_EXTERNAL_STORAGE` | 直接写入 `Download/FileShare`（可在设置页跳转授权） |
| `READ / WRITE_EXTERNAL_STORAGE` | Android 9 及以下的公共目录访问（`maxSdkVersion` 限制） |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 引导加入电池优化白名单 |

## 构建

环境要求：JDK 17+、Android SDK Platform 36（`compileSdk = 36`，`minSdk = 26`，`targetSdk = 36`）。

```powershell
.\gradlew :app:assembleDebug
```

```powershell
.\gradlew :app:installDebug
```

- Maven 仓库：`settings.gradle.kts` 中优先使用阿里云 `google` / `public` / `gradle-plugin` 镜像，官方仓库兜底。
- Gradle wrapper：`gradle/wrapper/gradle-wrapper.properties` 指向腾讯云镜像的 Gradle 8.14.3；若下载异常，可换成 `https://mirrors.huaweicloud.com/gradle/gradle-8.14.3-bin.zip`。
- 本地 SDK 路径写在 `local.properties` 的 `sdk.dir`（已在 `.gitignore` 中忽略）。

## 浏览器页面

`assets/web/` 来自 `docs/assets/`，由 `core:server` 的 `/static/*` 路由直接从 `AssetManager` 提供。更新页面时同步复制到 `app/src/main/assets/web/` 即可，无需改动服务端。

## 启动图标

自适应图标三层：

- `drawable/ic_launcher_background.xml`：蓝色竖向渐变（`#1B6DE2 → #3388F5`）。
- `drawable/ic_launcher_foreground.xml`：白色「希」字矢量轮廓（Noto Sans SC Bold 提取）。
- `drawable/ic_launcher_monochrome.xml`：Android 13+ 主题图标单色层。

## 已知限制

- Android 15+ 对 `BOOT_COMPLETED` 启动 `dataSync` 前台服务有限制，部分系统会拦截开机自启，需要在系统设置中允许自启动。
- 从最近任务划掉 App 属于 force-stop，系统不再派发开机广播，共享服务也会停止。
- 未授予「所有文件访问」时，共享目录回退到应用专属目录；此时其他设备仍可正常上传下载。
