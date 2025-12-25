plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2") 
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    mainClass.set("dev.perillo.serversmith.Launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "Packages the application as a standalone executable using jpackage"
    dependsOn("installDist")

    val inputDir = "build/install/ServerSmith/lib"
    val outputDir = "build/dist"
    val mainJar = "ServerSmith.jar"
    val mainClass = "dev.perillo.serversmith.Launcher"
    val appVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"
    val os = org.gradle.internal.os.OperatingSystem.current()

    doFirst {
        delete(outputDir)
        mkdir(outputDir)
    }

    val platformType = when {
        os.isMacOsX -> "dmg"
        os.isWindows -> "exe"
        else -> "deb" // Default to deb for Linux
    }

    val iconExt = when {
        os.isMacOsX -> "icns"
        os.isWindows -> "ico"
        else -> "png"
    }

    commandLine(mutableListOf(
        "jpackage",
        "--name", "ServerSmith",
        "--vendor", "Sam Perillo",
        "--description", "A professional server management and creation tool.",
        "--app-version", appVersion,
        "--main-jar", mainJar,
        "--main-class", mainClass,
        "--input", inputDir,
        "--dest", outputDir,
        "--icon", "src/main/resources/dev/perillo/serversmith/app-icon.$iconExt",
        "--type", platformType
    ).apply {
        if (os.isMacOsX) {
            addAll(listOf("--mac-package-name", "ServerSmith", "--mac-package-identifier", "dev.perillo.serversmith"))
        }
        if (os.isWindows) {
            addAll(listOf("--win-shortcut", "--win-menu"))
        }
        if (os.isLinux) {
            addAll(listOf("--linux-shortcut"))
        }
    })
}
