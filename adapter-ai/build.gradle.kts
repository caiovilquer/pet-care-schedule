plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
    `java-library`
}

kotlin { jvmToolchain(21) }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = "21" }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":application"))
    implementation("org.springframework:spring-context")
    implementation("io.micrometer:micrometer-core")
    implementation("org.apache.pdfbox:pdfbox:3.0.8")

    testImplementation(libs.junit.jupiter.api)
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test { useJUnitPlatform() }
