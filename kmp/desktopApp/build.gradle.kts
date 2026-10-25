import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "com.ttkk0000.meowcircle"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":clientUi"))
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.cmpMaterial3)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.ktorClientOkhttp)
    implementation("com.github.javakeyring:java-keyring:1.0.4")
}

compose.desktop {
    application {
        mainClass = "com.ttkk0000.meowcircle.desktop.MainKt"
        nativeDistributions {
            modules("jdk.httpserver")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MND Desktop"
            packageVersion = "1.0.0"
            description = "M&D meow & doggie desktop client."
            vendor = "meow & doggie"
        }
    }
}
