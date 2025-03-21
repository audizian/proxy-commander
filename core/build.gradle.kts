import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xalitoria.repo.*

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
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
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        from(sourceSets["main"].output)
    }
    build {
        dependsOn("shadowJar")
    }
}