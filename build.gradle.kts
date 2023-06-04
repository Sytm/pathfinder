import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  with(libs.plugins) {
    alias(kotlin)
    alias(spotless)
  }
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  api(libs.stdlib)
  api(libs.paper)
}

kotlin { jvmToolchain(libs.versions.jvmToolchain.get().toInt()) }

tasks.withType<KotlinCompile> {
  compilerOptions.freeCompilerArgs.addAll(
    "-Xjvm-default=all",
    "-Xlambdas=indy",
  )
}

spotless {
  kotlin {
    ktfmt()
  }
}

publishing {
  repositories {
    maven {
      name = "md5lukasReposilite"

      url =
          uri(
              "https://repo.md5lukas.de/${
            if (version.toString().endsWith("-SNAPSHOT")) {
              "snapshots"
            } else {
              "releases"
            }
          }")

      credentials(PasswordCredentials::class)
      authentication { create<BasicAuthentication>("basic") }
    }
  }
  publications { create<MavenPublication>("maven") { from(components["java"]) } }
}
