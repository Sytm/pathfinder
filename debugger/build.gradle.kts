import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
  with(libs.plugins) {
    alias(kotlin)
    alias(shadow)
    alias(runPaper)
    alias(spotless)
  }
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.md5lukas.de/public/")
}

dependencies {
  implementation(libs.stdlib)
  implementation(libs.paper)
  implementation(libs.dependencyLoader)
  implementation(project(":"))
}

kotlin { jvmToolchain(libs.versions.jvmToolchain.get().toInt()) }

tasks.withType<ProcessResources> {

  val properties = mapOf(
    "version" to version,
    "apiVersion" to libs.versions.paper.get().split('.').take(2).joinToString("."),
    "kotlinVersion" to libs.versions.kotlin.get()
  )

  inputs.properties(properties)

  filteringCharset = "UTF-8"

  filesMatching(listOf("paper-plugin.yml", "dependencies.yml")) {
    expand(properties)
  }
}

tasks.withType<KotlinCompile> {
  compilerOptions.freeCompilerArgs.addAll(
    "-Xjvm-default=all",
    "-Xlambdas=indy",
  )
}

tasks.withType<ShadowJar> {
  dependencies {
    include(dependency(libs.dependencyLoader.get()))
    include(project(":"))
  }
  relocate("de.md5lukas.paper.loader", "de.md5lukas.pathfinder.debugger")
}

spotless {
  kotlin {
    ktfmt()
  }
}

tasks.withType<RunServer> {
  minecraftVersion(libs.versions.paper.get().substringBefore('-'))
}
