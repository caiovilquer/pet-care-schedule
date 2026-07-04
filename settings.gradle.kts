rootProject.name = "petcarescheduler"
include("core", "application", "adapter-persistence", "adapter-messaging", "adapter-rest", "bootstrap")
pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}