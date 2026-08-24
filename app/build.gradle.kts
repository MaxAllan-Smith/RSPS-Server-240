import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    kotlin("jvm") version "2.4.10"
}

repositories {
    mavenCentral()
    maven("https://repo.openrs2.org/repository/openrs2-snapshots")
}

dependencies {
    // Revision-locked game protocol model/codec.
    implementation("net.rsprot:osrs-240-api:1.0.0-ALPHA-20260815")

    // OpenRS2 cache filesystem used by the existing cache/bootstrap services.
    implementation("org.openrs2:openrs2-cache:0.1.0-SNAPSHOT")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.1")
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

    // RuneScape-style BFS routefinding + directional collision semantics.
    implementation("org.rsmod:rsmod-routefinder:6.0.0")

    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(26)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_26)
    }
}

application {
    mainClass.set("org.example.app.AppKt")
    applicationDefaultJvmArgs =
        listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
        )
}

// Keep runtime data (.data/) at repository root instead of app/.
tasks.named<JavaExec>("run") {
    workingDir(rootProject.projectDir)
}

tasks.test {
    useJUnitPlatform()
}
