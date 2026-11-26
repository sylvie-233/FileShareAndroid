# build-logic · Gradle 约定插件

把 AGP / Kotlin / Compose 的公共配置收敛为四个约定插件，模块构建脚本只声明自身差异。

## 插件

| 插件 ID | 内容 |
| --- | --- |
| `fileshare.android.application` | `com.android.application` + Kotlin Android；`compileSdk 36`、`minSdk 26`、`targetSdk 36`、Java / Kotlin 17、`buildConfig`、打包去重 |
| `fileshare.android.library` | `com.android.library` + Kotlin Android；相同的 SDK / Java 配置与打包规则 |
| `fileshare.android.compose` | 应用 Compose 编译器插件；未应用 Android 插件时自动应用 library 约定；添加 Compose BOM、UI、Foundation、Material 3、图标、Activity Compose 与调试工具 |
| `fileshare.android.feature` | 组合 Compose 约定 + `core:designsystem`；添加 Lifecycle Compose、ViewModel、协程、Koin（Android / Compose / ViewModel） |

根工程 `build.gradle.kts` 以 `alias(...) apply false` 声明 Android / Kotlin 插件版本，使约定插件在子工程中能解析到 AGP。

## 版本与镜像

- 依赖版本集中在 [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)，约定插件通过 `VersionCatalogsExtension` 读取，模块构建脚本使用生成的 `libs` 访问器。
- Maven 仓库在根 [`settings.gradle.kts`](../settings.gradle.kts) 中优先使用阿里云 `google` / `public` / `gradle-plugin` 镜像，官方仓库兜底。
- Gradle 发行包由 [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties) 指向腾讯云镜像的 Gradle 8.14.3。

## 新增模块

Feature 模块：

```kotlin
plugins {
    id("fileshare.android.feature")
}

android {
    namespace = "com.fileshare.lan.feature.demo"
}

dependencies {
    implementation(project(":core:model"))
}
```

1. 在 `settings.gradle.kts` 中添加 `include(":feature:demo")`。
2. 按需依赖 `core:model` / `core:common` / `core:data` / `core:server`。
3. 在模块内提供 Screen、ViewModel 与 Koin `module`，并在 `app` 的 `FileShareApplication` 中注册。
4. 纯 Kotlin 模块（如 `core:model`、`core:common`）直接使用 `alias(libs.plugins.kotlin.jvm)`。

Core Android 模块使用 `fileshare.android.library`；需要 Compose 时改用 `fileshare.android.compose`，其依赖 BOM 由约定插件统一提供。

约定插件源码位于 `convention/src/main/kotlin/`，修改 `compileSdk`、`minSdk` 或通用依赖时只需改对应插件。
