package com.xalitoria.repo

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.sonatypeRepo() =
    maven("https://oss.sonatype.org/content/repositories/snapshots") { name = "sonatype" }

fun RepositoryHandler.spigotRepo() =
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "spigotmc-repo" }

fun RepositoryHandler.paperRepo() =
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }

fun RepositoryHandler.jitpack() =
    maven("https://jitpack.io") { name = "Jitpack" }