pluginManagement {
    includeBuild("build-logic")
    repositories {
        // 本地开发优先走国内镜像；CI（GitHub Actions）使用官方仓库，避免跨境拉取
        if (System.getenv("CI") != "true") {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (System.getenv("CI") != "true") {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "FileShare"

include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:data")
include(":core:server")
include(":feature:share")
include(":feature:files")
include(":feature:settings")
include(":app")
