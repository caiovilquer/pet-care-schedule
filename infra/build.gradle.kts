import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa") version "1.9.25"
}

kotlin { jvmToolchain(17) }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = "17" }
}

tasks.withType<BootJar>().configureEach {
    archiveFileName.set("petcare.jar")
}

springBoot {
    mainClass.set("dev.vilquer.petcarescheduler.PetCareSchedulerApplicationKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":usecase"))
    implementation(project(":application"))

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    runtimeOnly("org.postgresql:postgresql")
    // Perfil dev e testes de integração usam H2 em modo PostgreSQL
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
