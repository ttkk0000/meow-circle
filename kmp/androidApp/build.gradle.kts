import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

/**
 * 默认 `10.0.2.2`（官方 Android 模拟器）。
 * MuMu / 雷电等：在 **已有** `kmp/local.properties`（含 sdk.dir）里增加一行
 * `meow.api.base.url=http://127.0.0.1:8080`，并在电脑执行 `adb reverse tcp:8080 tcp:8080`。
 * 也可仅本次构建：`./gradlew :androidApp:assembleDebug -Pmeow.api.base.url=http://127.0.0.1:8080`
 */
val meowApiBaseUrl: String =
    (project.findProperty("meow.api.base.url") as String?)?.trim()?.takeIf { it.isNotEmpty() }
        ?: run {
            val f = rootProject.file("local.properties")
            if (!f.exists()) return@run "http://10.0.2.2:8080"
            val p = Properties()
            f.inputStream().use { p.load(it) }
            p.getProperty("meow.api.base.url")?.trim()?.takeIf { it.isNotEmpty() } ?: "http://10.0.2.2:8080"
        }

android {
    namespace = "com.ttkk0000.meowcircle.kmpapp"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ttkk0000.meowcircle.kmpapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"$meowApiBaseUrl\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)
    implementation(libs.composeMaterial3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxLifecycleViewmodelCompose)
}
