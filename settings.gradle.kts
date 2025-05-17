plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "petcarescheduler"
include("core")
include("usecase")
include("application")
include("infra")
