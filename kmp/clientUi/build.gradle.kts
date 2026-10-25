import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.ttkk0000.meowcircle.clientui"
        compileSdk = 37
        minSdk = 26
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.components.resources)
            implementation(libs.cmpMaterial3)
            implementation(libs.cmpIcons)
            implementation(libs.kotlinxCoroutinesCore)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.coilCompose)
            implementation(libs.coilSvg)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.ttkk0000.meowcircle.clientui.resources"
}
