plugins {
    id("fileshare.android.feature")
}

android {
    namespace = "com.fileshare.lan.feature.settings"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:server"))
    implementation(libs.androidx.core.ktx)
}
