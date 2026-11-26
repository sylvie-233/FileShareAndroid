import org.gradle.api.JavaVersion
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")

        // CI / 本地发布可用 -Pfileshare.versionName / -Pfileshare.versionCode 覆盖版本号
        val versionNameOverride = (findProperty("fileshare.versionName") as String?)?.takeIf { it.isNotBlank() }
        val versionCodeOverride = (findProperty("fileshare.versionCode") as String?)?.toIntOrNull()

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            buildToolsVersion = "36.0.0"

            defaultConfig {
                minSdk = 26
                targetSdk = 36
                versionCode = versionCodeOverride ?: 1
                versionName = versionNameOverride ?: "1.0.0"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            buildFeatures {
                buildConfig = true
            }

            packaging {
                resources {
                    excludes += setOf(
                        "/META-INF/{AL2.0,LGPL2.1}",
                        "/META-INF/INDEX.LIST",
                        "/META-INF/DEPENDENCIES",
                        "/META-INF/LICENSE*",
                        "/META-INF/NOTICE*",
                        "/META-INF/io.netty.versions.properties",
                        "META-INF/*.kotlin_module",
                    )
                }
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}
