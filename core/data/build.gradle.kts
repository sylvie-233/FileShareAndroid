plugins {
    id("fileshare.android.library")
}

android {
    namespace = "com.fileshare.lan.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
}
