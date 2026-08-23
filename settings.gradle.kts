/*
 * Gradle settings for RSPS_RSProt_Server.
 *
 * The build uses a standard application module directly rather than an
 * included build-logic convention-plugin project. This keeps the Gradle
 * setup simple and avoids the Kotlin DSL precompiled-script generation
 * issues previously coming from build-logic.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    /*
     * Allows Gradle toolchains to automatically resolve/download a
     * suitable JDK when one is not already installed locally.
     */
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "RSPS_RSProt_Server"

include("app")