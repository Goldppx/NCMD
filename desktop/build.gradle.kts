plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.jaudiotagger)
    implementation(libs.material.kolor)
    implementation(libs.mp3spi)
    implementation(libs.vorbisspi)
    implementation(libs.jflac)
}

compose.desktop {
    application {
        mainClass = "com.gem.neteasecloudmd.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "NCMD"
            packageVersion = "0.0.8"
            description = "A Kotlin Multiplatform music client"
            vendor = "NCMD"
        }
    }
}
