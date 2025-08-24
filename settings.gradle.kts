rootProject.name = "petcarescheduler"
include("core", "usecase", "application", "infra")
pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}