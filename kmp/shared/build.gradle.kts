import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
}

group = "com.ttkk0000.meowcircle"
version = "0.1.0"

kotlin {
    android {
        namespace = "com.ttkk0000.meowcircle.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktorClientCore)
            implementation(libs.ktorClientContentNegotiation)
            implementation(libs.ktorSerializationKotlinxJson)
            implementation(libs.kotlinxCoroutinesCore)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.multiplatformSettings)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktorClientMock)
            implementation(libs.kotlinxCoroutinesTest)
        }
        androidMain.dependencies {
            implementation(libs.ktorClientOkhttp)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.ktorClientOkhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktorClientDarwin)
        }
    }
}
