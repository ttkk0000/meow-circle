plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "com.ttkk0000.meowcircle"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.ttkk0000.meowcircle.desktop.MainKt"
        nativeDistributions {
            // targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MND Desktop"
            packageVersion = "1.0.0"
            description = "M&D desktop client aligned to the Stitch V2 design system."
            vendor = "meow & doggie"
        }
    }
}

rootProject.tasks.matching { it.name.contains("wix", ignoreCase = true) }.configureEach {
    enabled = false
}
