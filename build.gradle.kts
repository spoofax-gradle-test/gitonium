import mb.gitonium.GitoniumExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  // Stick with version 1.3.10 because the kotlin-dsl plugin uses that.
  kotlin("jvm") version "1.3.10" apply true
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
  id("org.metaborg.gitonium") version "0.3.0"
}

group = "org.metaborg"

repositories {
  maven(url = "http://home.gohla.nl:8091/artifactory/all/")
}

dependencies {
  compile(kotlin("stdlib"))
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
tasks.withType<KotlinCompile>().all {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("gitonium") {
      id = "org.metaborg.gitonium"
      implementationClass = "mb.gitonium.GitoniumPlugin"
    }
  }
}

tasks {
  register("buildAll") {
    dependsOn("build")
  }
  register("cleanAll") {
    dependsOn("clean")
  }
}

publishing {
  repositories {
    maven {
      name = "Artifactory"
      url = uri("http://192.168.1.3:8091/artifactory/all/")
      credentials {
        username = project.findProperty("publish.repository.Artifactory.username")?.toString()
        password = project.findProperty("publish.repository.Artifactory.password")?.toString()
      }
    }
  }
}