plugins {
    id("fileshare.android.compose")
}

android {
    namespace = "com.fileshare.lan.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
}
