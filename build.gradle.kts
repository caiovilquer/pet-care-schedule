// build.gradle.kts (raiz)

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false

    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
}

allprojects {
    repositories { mavenCentral() }
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }
    }
}
