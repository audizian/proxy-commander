import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@OptIn(ExperimentalKotlinGradlePluginApi::class)

plugins {
    `kotlin-dsl` // Enables Kotlin DSL support
    kotlin("jvm") version "2.1.20-RC"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(gradleApi()) // Gradle API for classes like RepositoryHandler
    implementation(localGroovy()) // Groovy support for Gradle
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDirs("kotlin")
            resources.srcDirs("resources")
        }
    }
    jvmToolchain(21)
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(21)
}
