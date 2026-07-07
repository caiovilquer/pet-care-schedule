plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
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

    // Só o cliente HTTP síncrono (RestClient) — o starter-webflux anterior
    // arrastava Netty+Reactor inteiros para duas chamadas bloqueantes de
    // e-mail, custando dezenas de MB de memória em idle. spring-context,
    // spring-boot e slf4j vinham transitivos do starter e agora são diretos.
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.slf4j:slf4j-api")
    // Parsing do JSON (snake_case) da API legada do Google via JsonNode,
    // isolado do ObjectMapper Jackson global da aplicação (que serializa a
    // API REST em camelCase) — evita acoplar as duas convenções de naming.
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    // Gradle empacota seu próprio junit-platform-launcher, que fica desalinhado
    // com o junit-platform-engine mais novo trazido pelo BOM do Spring Boot;
    // declarar a versão do BOM explicitamente resolve o conflito.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
