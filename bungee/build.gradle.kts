import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xalitoria.repo.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@OptIn(ExperimentalKotlinGradlePluginApi::class)
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.bungeeyml)
}

repositories {
    mavenCentral()
    mavenLocal()
    sonatypeRepo()
    spigotRepo()
    paperRepo()
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.textcolorutil)
    implementation(project(":core"))

    compileOnly(libs.bungeecord)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDirs("kotlin")
            resources.srcDirs("resources")
        }
    }
    jvmToolchain(findProperty("java")?.toString()?.toIntOrNull() ?: 21)
}

bungee {
    name = rootProject.name
    main = "com.xalitoria.commander.BungeecordPlugin"
    author = "audizian"
    version = project.version.toString()
    description = project.property("bungee.description").toString()
}

tasks {
    jar { enabled = false }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveClassifier = ""
        dependsOn(":core:shadowJar")
    }
    build {
        dependsOn("generateBungeePluginDescription")
        dependsOn(shadowJar)
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}