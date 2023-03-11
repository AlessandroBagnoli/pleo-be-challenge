plugins {
  kotlin("jvm")
  id("org.unbroken-dome.test-sets")
}

kotlinProject()

dependencies {
  implementation(project(":pleo-antaeus-data"))
  api(project(":pleo-antaeus-models"))
}

testSets {
  "functional"()
}

tasks.getByName("check").dependsOn(tasks.named("functional"))