import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  // Stick with version 1.3.10 because the kotlin-dsl plugin uses that.
  kotlin("jvm") version "1.3.10" apply true
  `kotlin-dsl`
  `java-gradle-plugin`
}

group = "org.metaborg"
version = "develop-SNAPSHOT"

repositories {
  mavenCentral()
  jcenter()
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
