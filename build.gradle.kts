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
    mainClass.set("dev.perillo.serversmith.App")
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
    val mainClass = "dev.perillo.serversmith.App"

    doFirst {
        delete(outputDir)
        mkdir(outputDir)
    }

    commandLine(
        "jpackage",
        "--name", "ServerSmith",
        "--vendor", "Sam Perillo",
        "--description", "A professional server management and creation tool.",
        "--app-version", "1.0.0",
        "--main-jar", mainJar,
        "--main-class", mainClass,
        "--input", inputDir,
        "--dest", outputDir,
        "--icon", "src/main/resources/dev/perillo/serversmith/app-icon." + (if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) "icns" else "png"),
        "--type", if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) "dmg" else "exe",
        "--mac-package-name", "ServerSmith"
    )
}
