# feature:files · 文件页

共享文件的浏览与管理：搜索、分页、上传进度、下载到本机、复制直链、删除与清空。

![文件页](../../docs/screenshots/app-files.png)

## 布局

```text
搜索框 + 上传按钮
上传进度（有任务时）
共 N 个文件 + 清空全部
文件列表卡片（右侧滚动条）
页码分页
```

- 搜索框 40dp 描边样式；输入后 **150ms 防抖**，切换关键词或翻页时列表回到顶部。
- 文件行：类型徽章（文档 / 图片 / 音视频 / 压缩包 / APK / 代码独立配色）、单行文件名、大小与相对时间。
- 每页 8 条，页码按钮在页数较多时自动收缩为「首尾 + 当前页附近」。
- 列表内容超出卡片高度时显示右侧滚动条，滚动时加深，内容不足时隐藏。

## 文件操作

| 操作 | 行为 |
| --- | --- |
| 下载 | 打开系统「另存为」面板（`CreateDocument`），把共享文件复制到用户选择的位置，完成后 Toast |
| 复制链接 | 服务运行时复制 `http://ip:port/api/v1/download/<filename>`；未运行提示先启动服务 |
| 删除 / 清空 | 二次确认后调用 `SharedStorage`，删除成功后刷新列表 |
| 上传 | 通过 `OpenDocument` 选择本机文件并导入共享目录，显示实时进度 |

## 实现

`FilesViewModel` 维护 `FilesUiState`（列表、总数、页码、查询、上传任务、存储错误），并：

- 监听 `settings.saveDirUri` 与刷新计数，切换目录后重新加载；
- 订阅 `ShareController.uploads` 展示浏览器上传进度；
- 订阅服务端 `FilesChanged` 事件自动刷新；
- 搜索在 ViewModel 内做 150ms 防抖与分页裁剪，翻页时自动收敛页码；
- 下载通过 `ActivityResultContracts.CreateDocument("application/octet-stream")` 获取目标 URI，再由 `ContentResolver.openOutputStream` 写出。
