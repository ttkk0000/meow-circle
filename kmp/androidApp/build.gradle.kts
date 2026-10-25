import java.util.Properties
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
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

// 发布凭据只从构建环境注入，不回退到debug签名。
val releaseStore = providers.environmentVariable("MND_ANDROID_KEYSTORE").orNull
val releaseAlias = providers.environmentVariable("MND_ANDROID_KEY_ALIAS").orNull
val releaseStorePassword = providers.environmentVariable("MND_ANDROID_STORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("MND_ANDROID_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(releaseStore,releaseAlias,releaseStorePassword,releaseKeyPassword).all { !it.isNullOrBlank() }
val releaseVersionCode = providers.gradleProperty("meow.version.code").orElse(providers.environmentVariable("MND_VERSION_CODE")).orElse("1").get().toInt()
val releaseVersionName = providers.gradleProperty("meow.version.name").orElse(providers.environmentVariable("MND_VERSION_NAME")).orElse("0.1.0").get()
require(releaseVersionCode > 0 && releaseVersionName.matches(Regex("[0-9]+(\\.[0-9]+){1,2}([a-zA-Z0-9.-]*)"))) { "Invalid application version" }
val requireReleaseConfiguration = tasks.register("requireReleaseConfiguration") {
    doLast {
        check(releaseSigningReady) { "Release requires MND_ANDROID_KEYSTORE, KEY_ALIAS, STORE_PASSWORD and KEY_PASSWORD" }
        check(file(checkNotNull(releaseStore)).isFile) { "Release keystore is missing" }
        val endpoint = java.net.URI(meowApiBaseUrl)
        check(endpoint.scheme == "https" && !endpoint.host.isNullOrBlank() && !endpoint.host.endsWith(".invalid") && endpoint.host !in setOf("localhost", "127.0.0.1", "::1") && endpoint.userInfo == null && endpoint.rawQuery == null && (endpoint.path.isNullOrEmpty() || endpoint.path == "/") && endpoint.fragment == null) { "Release requires an HTTPS API endpoint without credentials" }
    }
}
tasks.configureEach {
    if (name in setOf("assembleRelease", "bundleRelease", "packageRelease", "validateSigningRelease")) dependsOn(requireReleaseConfiguration)
}

// 平台Key由本机构建属性／环境注入，不把真实值写入仓库。
val meowAmapKey = providers.gradleProperty("meow.amap.android.key")
    .orElse(providers.environmentVariable("MND_AMAP_ANDROID_KEY")).orElse("").get().trim()

// 仅注入Firebase公开应用配置；没有配置时不初始化FCM，不需要google-services.json。
val fcmConfiguration = mapOf(
    "FCM_APP_ID" to providers.environmentVariable("MND_FCM_ANDROID_APP_ID").orElse("").get(),
    "FCM_PROJECT_ID" to providers.environmentVariable("MND_FCM_PROJECT_ID").orElse("").get(),
    "FCM_SENDER_ID" to providers.environmentVariable("MND_FCM_SENDER_ID").orElse("").get(),
    "FCM_API_KEY" to providers.environmentVariable("MND_FCM_ANDROID_API_KEY").orElse("").get(),
)
require(fcmConfiguration.values.all { it.matches(Regex("[A-Za-z0-9_:-]{0,200}")) }) { "Invalid Firebase public configuration" }
val fcmConfigured = fcmConfiguration.values.all { it.isNotBlank() }
require(fcmConfiguration.values.all { it.isBlank() } || fcmConfigured) { "Provide all four Firebase public configuration values" }

val meowWechatAppId = providers.gradleProperty("meow.wechat.app.id").orElse(providers.environmentVariable("MND_WECHAT_ANDROID_APP_ID")).orElse("wx0000000000000000").get().trim().ifBlank { "wx0000000000000000" }
val meowQqAppId = providers.gradleProperty("meow.qq.app.id").orElse(providers.environmentVariable("MND_QQ_ANDROID_APP_ID")).orElse("00000000").get().trim().ifBlank { "00000000" }
require(meowWechatAppId.matches(Regex("wx[a-zA-Z0-9]{16}"))) { "Invalid WeChat AppID" }
require(meowQqAppId == "00000000" || meowQqAppId.matches(Regex("[1-9][0-9]{4,19}"))) { "Invalid QQ AppID" }

/** QQ 3.5.12后仅官方ZIP分发；构建时按固定摘要取唯一JAR，不使用第三方重打包。当前任务未执行。 */
abstract class PrepareOfficialQqSdk : DefaultTask() {
    @get:OutputFile abstract val output: RegularFileProperty
    @TaskAction fun prepare() {
        val connection = java.net.URI("https://tangram-1251316161.file.myqcloud.com/files/20260414/143310b667ece8922594fbe866dabdef.zip").toURL().openConnection().apply {
            connectTimeout = 15000; readTimeout = 30000
        }
        val archive = connection.getInputStream().use { it.readNBytes(5 * 1024 * 1024 + 1) }
        check(archive.size <= 5 * 1024 * 1024)
        val digest = MessageDigest.getInstance("SHA-256").digest(archive).joinToString("") { "%02x".format(it) }
        check(digest == "6272d4123d8327400a655c48f1fb7eb24b83fd5beeb527951ed100b219192b9b") { "QQ SDK archive digest changed" }
        var jar: ByteArray? = null
        ZipInputStream(archive.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if(entry.name == "open_sdk_3.5.19_r9483ffc7_lite.jar") {
                    check(jar == null) { "Duplicate QQ SDK JAR" }
                    jar = zip.readNBytes(5 * 1024 * 1024 + 1)
                }
                entry = zip.nextEntry
            }
        }
        val bytes = checkNotNull(jar)
        check(bytes.size <= 5 * 1024 * 1024)
        output.get().asFile.apply { parentFile.mkdirs(); writeBytes(bytes) }
    }
}
val prepareOfficialQqSdk = tasks.register<PrepareOfficialQqSdk>("prepareOfficialQqSdk") {
    output.set(layout.buildDirectory.file("generated/official-qq/open-sdk-3.5.19.jar"))
}

android {
    namespace = "com.ttkk0000.meowcircle.kmpapp"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.ttkk0000.meowcircle.kmpapp"
        minSdk = 26
        targetSdk = 37
        manifestPlaceholders["allowCleartextTraffic"] = "false"
        manifestPlaceholders["appLinkHost"] = java.net.URI(meowApiBaseUrl).host ?: "localhost"
        manifestPlaceholders["amapApiKey"] = meowAmapKey
        buildConfigField("boolean", "AMAP_CONFIGURED", meowAmapKey.isNotEmpty().toString())
        buildConfigField("String", "WECHAT_APP_ID", "\"$meowWechatAppId\"")
        buildConfigField("String", "QQ_APP_ID", "\"$meowQqAppId\"")
        manifestPlaceholders["qqAuthScheme"] = "tencent$meowQqAppId"
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        buildConfigField("String", "API_BASE_URL", "\"$meowApiBaseUrl\"")
        buildConfigField("boolean", "FCM_CONFIGURED", fcmConfigured.toString())
        fcmConfiguration.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
    }
    signingConfigs {
        if(releaseSigningReady) create("release") {
            storeFile = file(checkNotNull(releaseStore))
            storePassword = releaseStorePassword
            keyAlias = releaseAlias
            keyPassword = releaseKeyPassword
        }
    }
    buildTypes {
        debug {
            manifestPlaceholders["allowCleartextTraffic"] = "true"
        }
        release {
            if(releaseSigningReady) signingConfig = signingConfigs.getByName("release")
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
    implementation("com.google.firebase:firebase-messaging:26.0.1")
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.40")
    implementation(files(prepareOfficialQqSdk.flatMap { it.output }))
    implementation(project(":clientUi"))
    // 官方现行Maven合包；仅初始化地图，不启动定位／搜索客户端。
    implementation("com.amap.api:3dmap-location-search:11.3.100_loc11.3.101_sea9.8.1")
    implementation(project(":shared"))
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)
    implementation(libs.composeMaterial3)
    implementation(libs.composeIcons)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.coilCompose)
    implementation(libs.coilNetwork)
    implementation(libs.coilSvg)
    implementation(libs.androidxExif)
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation("androidx.window:window:1.5.1")
}
