import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xalitoria.repo.*
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@OptIn(ExperimentalKotlinGradlePluginApi::class)
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml)
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
    implementation(libs.kotlin.coroutines.jvm)
    implementation(libs.textcolorutil)
    implementation(project(":core"))

    compileOnly(libs.spigot)
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

bukkit {
    name = rootProject.name
    author = "audizian"
    main = "com.xalitoria.commander.ServerPlugin"
    version = project.version.toString()
    description = project.property("spigot.description").toString()
    website = "https://xalitoria.com"
    foliaSupported = true
    apiVersion = "1.13"
    load = PluginLoadOrder.POSTWORLD

    Command("proxycommander").apply {
        aliases = listOf("prex", "pcmd")
        permission = "proxycmd.execute"
        usage = "/<command> <\"command\">"
    }.let { commands.add(it) }
    Command("serveralias").apply {
        permission = "proxycmd.server"
        usage = "/<command> <server>"
    }.let { commands.add(it) }
    Command("pcmd-sync").apply {
        permission = "proxycmd.sync"
        usage = "/<command>"
    }.let { commands.add(it) }
}

tasks {
    jar { enabled = false }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveClassifier = ""
        dependsOn(":core:shadowJar")
    }
    build {
        dependsOn("generateBukkitPluginDescription")
        dependsOn(shadowJar)
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}