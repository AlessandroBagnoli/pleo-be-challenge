plugins {
  kotlin("jvm")
}

kotlinProject()

webLibs()

dependencies {
  implementation(project(":pleo-antaeus-core"))
  implementation(project(":pleo-antaeus-models"))
}
