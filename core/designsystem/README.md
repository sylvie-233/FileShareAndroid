# core:designsystem · 设计系统

按交互原型 v1 实现的 Material 3 主题与通用 Compose 组件，供所有 feature 模块复用。

## 主题令牌

`theme/Color.kt` 中的 `AppColors`：

| 令牌 | 色值 | 用途 |
| --- | --- | --- |
| `Background` | `#EEF1F5` | 页面背景 |
| `Surface` / `SurfaceMuted` | `#FFFFFF` / `#F4F6F9` | 卡片、输入框底色 |
| `Border` / `BorderStrong` | `#E3E8EE` / `#CFD7E0` | 分隔线与描边 |
| `Ink` / `Muted` / `Faint` | `#171E28` / `#6B7686` / `#98A2B3` | 主 / 次 / 辅助文字 |
| `Accent` / `AccentSoft` | `#3B6EF6` / `#ECF1FE` | 主色与浅底 |
| `Green` / `GreenSoft` | `#1FA971` / `#E6F6EF` | 运行状态、成功 |
| `Danger` / `DangerSoft` | `#D9483B` / `#FDEEEA` | 危险操作 |
| `Amber` / `AmberSoft` | `#D9922B` / `#FDF3E3` | 警告提示 |

`FileShareTheme` 固定使用浅色方案（v1 不支持深色模式），圆角以 14dp 为主。

## 组件

| 组件 | 说明 |
| --- | --- |
| `AppCard` | 白底、1dp 描边、14dp 圆角的卡片容器 |
| `SectionLabel` | 分组小标题 |
| `StatusChip` | 顶部运行状态徽章 |
| `ConfirmDialog` | 危险操作二次确认弹窗 |
| `SegmentedOptions` | 端口 / 大小上限等分段选择器 |
| `SettingSwitchRow` | 标题 + 说明 + Switch 的设置行 |
| `InfoBanner` | WARN / INFO / SUCCESS 三色信息横幅 |
| `EmptyState` | 列表空状态与无结果占位 |
| `LabelValueRow` | 左右对齐的标签值行 |

## 使用

```kotlin
FileShareTheme {
    AppCard {
        SectionLabel("服务")
        SettingSwitchRow(
            title = "开机自动启动服务",
            description = "设备开机后自动开启共享",
            checked = settings.autostart,
            onCheckedChange = viewModel::setAutostart,
        )
    }
}
```
