import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 业务 feature 模块的通用约定：Compose UI + ViewModel + Koin + 协程。
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("fileshare.android.compose")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
            add("implementation", libs.findLibrary("koin-android").get())
            add("implementation", libs.findLibrary("koin-compose").get())
            add("implementation", libs.findLibrary("koin-compose-viewmodel").get())
        }
    }
}
