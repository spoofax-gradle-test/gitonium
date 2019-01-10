import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.metaborg.gradle.config") version "0.4.1"
  id("org.metaborg.gitonium") version "0.3.0" // Bootstrap with previous version.
  kotlin("jvm") version "1.3.11" // Use version 1.3.11 for compatibility with Gradle 5.1.
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
}

// TODO: configureKotlinGradlePlugin() causes compilation errors in plugin.
//metaborgConfig {
//  configureKotlinGradlePlugin()
//}

dependencies {
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
gradlePlugin {
  plugins {
    create("gitonium") {
      id = "org.metaborg.gitonium"
      implementationClass = "mb.gitonium.GitoniumPlugin"
    }
  }
}
