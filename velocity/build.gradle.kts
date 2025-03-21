import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xalitoria.repo.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@OptIn(ExperimentalKotlinGradlePluginApi::class)
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    mavenLocal()
    sonatypeRepo()
    paperRepo()
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.textcolorutil)
    implementation(project(":core"))

    compileOnly(libs.velocity)
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

tasks {
    jar { enabled = false }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveClassifier = ""
        dependsOn(":core:shadowJar")
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val props = mapOf(
            "name" to rootProject.name,
            "id" to rootProject.name.lowercase(),
            "version" to project.version.toString(),
            "description" to project.property("velocity.description").toString(),
            "authors" to project.property("author").toString()
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }
}