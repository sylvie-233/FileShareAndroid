plugins {
    id("fileshare.android.application")
    id("fileshare.android.compose")
}

android {
    namespace = "com.fileshare.lan"

    defaultConfig {
        applicationId = "com.fileshare.lan"
    }

    // CI 中通过 Secrets 注入签名材料；本地未配置时 release 包不签名（CI 会回退到 debug 包）
    val releaseKeystorePath = System.getenv("FILESHARE_KEYSTORE_PATH")
    val releaseSigning = if (!releaseKeystorePath.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystorePath)
            storePassword = System.getenv("FILESHARE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("FILESHARE_KEY_ALIAS")
            keyPassword = System.getenv("FILESHARE_KEY_PASSWORD")
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:server"))

    implementation(project(":feature:share"))
    implementation(project(":feature:files"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
