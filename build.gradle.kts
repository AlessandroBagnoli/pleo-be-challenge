import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  kotlin("jvm") version "1.8.10" apply false
  id("org.unbroken-dome.test-sets") version "4.0.0"
}

allprojects {
  group = "io.pleo"
  version = "1.0"

  repositories {
    mavenCentral()
  }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}