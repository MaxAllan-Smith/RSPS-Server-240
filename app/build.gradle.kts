import org.gradle.api.tasks.JavaExec

plugins {
    id("buildlogic.kotlin-application-conventions")
}

repositories {
    mavenCentral()

    maven(
        "https://repo.openrs2.org/repository/openrs2-snapshots"
    )
}

dependencies {
    implementation(
        "net.rsprot:osrs-240-api:1.0.0-ALPHA-20260815"
    )

    implementation(
        "org.openrs2:openrs2-cache:0.1.0-SNAPSHOT"
    )

    implementation(
        "com.fasterxml.jackson.module:jackson-module-kotlin:2.19.1"
    )

    runtimeOnly(
        "ch.qos.logback:logback-classic:1.5.18"
    )
}

application {
    mainClass = "org.example.app.AppKt"

    /*
     * JDK 26 warns when JNR/JFFI attempts to load OpenRS2's
     * optional native bzip2 implementation.
     *
     * Allow unnamed modules to use native access.
     */
    applicationDefaultJvmArgs =
        listOf(
            "--enable-native-access=ALL-UNNAMED",
        )
}

/*
 * Gradle's application plugin normally executes :app:run with
 * app/ as the working directory.
 *
 * Force the repository root instead so:
 *
 *     .data/
 *
 * lives beside gradlew rather than underneath app/.
 */
tasks.named<JavaExec>("run") {
    workingDir(rootProject.projectDir)
}