# core:common · 通用工具

与 Android 框架解耦的 Kotlin JVM 工具集，覆盖格式化、文件名安全、设备识别与局域网地址探测。

## 内容

| 文件 | 能力 |
| --- | --- |
| `Formatters.kt` | `formatBytes`、`formatSpeed`、`formatUptime`、`formatRelativeTime`、`formatAbsoluteTime` |
| `FileTypes.kt` | `FileTone` 色系、扩展名解析、徽章文字、类型归类（文档 / 图片 / 音视频 / 压缩包 / APK / 代码） |
| `FileNames.kt` | 安全文件名校验、multipart 文件名清洗、UTF-8 乱码修复、同名序号、URL 段编码、RFC 5987 `Content-Disposition` |
| `DeviceAgentParser.kt` | 从 User-Agent 推断 iPhone / iPad / Android / Windows / Mac |
| `NetworkUtils.kt` | 获取本机局域网 IPv4：优先 wlan / 热点 / eth，排除回环、链路本地与虚拟网卡 |

## 关键行为

- `sanitizeFileName` 会修复部分客户端把 UTF-8 文件名按 Latin-1 解码产生的乱码；随后去掉路径前缀与非法字符，拒绝 `.`、`..`、隐藏文件名。
- `contentDisposition` 同时输出 ASCII 回退名与 `filename*=UTF-8''...`，保证中文文件名在各浏览器下正确下载。
- `fileTone` 在文件列表徽章中用于配色，APK 单独归类（区别于普通压缩包）。

## 测试

```powershell
.\gradlew :core:common:test
```

覆盖文件名安全与乱码修复、格式化、UA 解析、徽章类型映射。
