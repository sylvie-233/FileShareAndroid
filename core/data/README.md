# core:data · 设置与文件存储

设置持久化（DataStore）与共享目录访问（File / SAF 双实现），并向上提供统一的 `SettingsRepository`、`SharedStorage`。

## 设置

`DataStoreSettingsRepository` 使用名为 `settings` 的 Preferences DataStore：

| Key | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `port` | Int | `8080` | 服务端口 |
| `autostart` | Boolean | `true` | 开机自启 |
| `keep_awake` | Boolean | `true` | 共享时屏幕常亮 |
| `max_file_size` | Long | `100 MB` | `0` 表示不限 |
| `allow_upload` | Boolean | `true` | 是否允许浏览器上传 |
| `save_dir_uri` | String? | `null` | SAF tree URI；为空时使用默认目录 |

DataStore 读取遇到 `IOException` 会回退到空 Preferences，避免设置文件损坏导致启动失败。`SettingsRepository.current()` 供服务启动、开机广播等非 UI 场景同步读取。

## 文件存储

`SharedStorage` 抽象：

```kotlin
suspend fun list(): List<SharedFile>
suspend fun openRead(fileName: String): InputStream?
suspend fun save(fileName, input, expectedSize, maxBytes, onProgress): SharedFile
suspend fun delete(fileName: String): Boolean
suspend fun clearAll(): Int
suspend fun freeSpaceBytes(): Long?
```

| 实现 | 适用场景 | 行为 |
| --- | --- | --- |
| `FileSystemStorage` | 默认目录或已授予「所有文件访问」 | 流式写入隐藏临时文件，原子重命名；同名自动追加 `(1)`、`(2)`；写入前按 `Content-Length` 检查上限与剩余空间 |
| `SafStorage` | 用户在设置页选择的 SAF 目录 | 通过 `DocumentFile` 创建临时文档并重命名；无法查询剩余空间（返回 null） |

共同约定：

- 列表跳过隐藏文件与 `.upload-*.part` 临时文件。
- 超过 `maxBytes` 抛 `FileTooLargeException`；空间不足抛 `InsufficientStorageException`。
- 写入失败会删除临时文件；断点/续传不在 v1 范围内。

`StorageProvider` 负责按 URI 选择并缓存实现：

- 未设置 SAF 时：API 30+ 且已授予「所有文件访问」→ `Download/FileShare`；否则 → `Android/data/com.fileshare.lan/files/FileShare`。
- API 29 及以下使用 `WRITE_EXTERNAL_STORAGE` 判断公共目录可用性。

Koin 模块 `dataModule` 提供 `SettingsRepository` 与 `StorageProvider` 单例。
