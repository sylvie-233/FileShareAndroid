plugins {
    id("fileshare.android.application")
    id("fileshare.android.compose")
}

android {
    namespace = "com.fileshare.lan"

    defaultConfig {
        applicationId = "com.fileshare.lan"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
