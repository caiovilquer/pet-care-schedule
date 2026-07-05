import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
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
    implementation(project(":application"))
    implementation(project(":adapter-rest"))
    implementation(project(":adapter-persistence"))
    implementation(project(":adapter-messaging"))

    implementation("org.springframework.boot:spring-boot-starter")
    // SmokeE2ETest fala HTTP diretamente (TestRestTemplate/HttpEntity); os
    // controllers de fato vêm do adapter-rest, mas o compile classpath do
    // próprio bootstrap precisa dos tipos de spring-web.
    implementation("org.springframework.boot:spring-boot-starter-web")
    // SmokeE2ETest inspeciona ReminderOutboxJpaRepository diretamente para
    // verificar o outbox contra o banco real, não só com fakes.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Garante que os schedulers rodem em uma única instância quando houver
    // mais de uma réplica do bootstrap no ar (ver AUDITORIA.md).
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")

    runtimeOnly("org.postgresql:postgresql")
    // Perfil dev e testes de integração usam H2 em modo PostgreSQL
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // TestRestTemplate cai para o HttpURLConnection da JDK sem um client HTTP
    // dedicado no classpath; esse client lança HttpRetryException em qualquer
    // resposta 401 recebida com corpo em modo streaming (bug/limitação da JDK,
    // não do código de produção — confirmado batendo direto com curl).
    testImplementation("org.apache.httpcomponents.client5:httpclient5")

    // Testes de arquitetura (fronteira hexagonal por módulo)
    testImplementation("com.lemonappdev:konsist:0.17.3")
}

tasks.test {
    useJUnitPlatform()
}
