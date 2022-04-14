import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.compose") version "1.0.0"
}

dependencies {
    api(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("com.h2database:h2:2.1.210")
    implementation("com.google.guava:guava:31.1-jre")
}

compose.desktop {
    application {
        mainClass = "org.hertsig.commander.AppKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "commander"
            packageVersion = "1.0.0"
        }
    }
}

