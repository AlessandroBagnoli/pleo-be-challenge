plugins {
  kotlin("jvm")
  id("org.unbroken-dome.test-sets")
}

kotlinProject()

dataLibs()

dependencies {
  api(project(":pleo-antaeus-models"))
}

testSets {
  "functional"()
}

tasks.getByName("check").dependsOn(tasks.named("functional"))