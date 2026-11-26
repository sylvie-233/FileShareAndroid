# feature:settings · 设置页

服务、传输与系统权限的配置入口。

![设置页](../../docs/screenshots/app-settings.png)

## 设置项

| 分组 | 设置 | 行为 |
| --- | --- | --- |
| 服务 | 端口 `8080 / 8000 / 9090` | 运行中修改会提示并自动重启服务 |
| 服务 | 开机自动启动 | 写入 `autostart`，供 `BootReceiver` 读取 |
| 服务 | 共享时保持屏幕常亮 | 控制 Activity 的 `FLAG_KEEP_SCREEN_ON` |
| 传输 | 单文件大小上限 `100 MB / 1 GB / 不限` | 服务端上传时校验，超出返回 413 |
| 传输 | 允许其他设备上传 | 关闭后网页端隐藏上传区，服务端 403 兜底 |
| 传输 | 保存目录 | 系统文件夹选择器（SAF），持久化授权；可恢复默认 |
| 系统权限 | 所有文件访问 | 跳转 `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`，失败回退全局列表 |
| 系统权限 | 忽略电池优化 | 跳转 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`，失败回退设置页 |

## 实现

- `SettingsViewModel`：把 `SettingsRepository` 映射为 `StateFlow<AppSettings>`，提供各 setter；端口变更时若服务运行中调用 `ShareController.restartIfRunning()`。
- 保存目录：`ActivityResultContracts.OpenDocumentTree` 选择后调用 `takePersistableUriPermission(READ | WRITE)`，把 tree URI 写入 `save_dir_uri`；设置页展示 `StorageProvider` 解析后的目录名。
- 权限状态：`所有文件访问` 通过 `Environment.isExternalStorageManager()` 判断；`忽略电池优化` 通过 `PowerManager.isIgnoringBatteryOptimizations()` 判断，页面 `ON_RESUME` 时重新检测（从系统设置返回后即时刷新）。
- 所有修改即时写入 DataStore，无需手动保存；除端口外的设置对下一次请求立即生效。
