plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "MndMobileUI"
            isStatic = true
            export(project(":shared"))
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.cmpMaterial3)
            implementation(libs.cmpIcons)
            implementation(libs.kotlinxCoroutinesCore)
        }
    }
}
