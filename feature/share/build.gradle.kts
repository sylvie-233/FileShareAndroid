plugins {
    id("fileshare.android.feature")
}

android {
    namespace = "com.fileshare.lan.feature.share"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:server"))
    implementation(libs.zxing.core)
    implementation(libs.androidx.core.ktx)
}
