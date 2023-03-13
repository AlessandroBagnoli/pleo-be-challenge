import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

private const val coroutinesVersion = "1.6.4"
private const val googleCloudPubsubVersion = "1.123.5"
private const val gsonVersion = "2.10.1"
private const val junitVersion = "5.9.2"
private const val testcontainersVersion = "1.17.6"
private const val awaitilityVersion = "4.2.0"
private const val slf4jSimpleVersion = "2.0.6"
private const val kotlinLoggingVersion = "3.0.5"
private const val mockkVersion = "1.13.4"

private const val exposedVersion = "0.17.14"
private const val postgresqlVersion = "42.5.4"

private const val javalinVersion = "5.4.2"

/**
 * Configures the current project as a Kotlin project by adding the Kotlin `stdlib` as a dependency.
 */
fun Project.kotlinProject() {
  dependencies {
    // Kotlin libs
    "implementation"(kotlin("stdlib"))

    // Coroutines
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // PubSub client library
    "implementation"("com.google.cloud:google-cloud-pubsub:$googleCloudPubsubVersion")

    // GSON for Json de/serialization
    "implementation"("com.google.code.gson:gson:$gsonVersion")

    // Logging
    "implementation"("org.slf4j:slf4j-simple:$slf4jSimpleVersion")
    "implementation"("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    // Mockk
    "testImplementation"("io.mockk:mockk:$mockkVersion")

    // JUnit 5
    "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    // Testcontainers
    "testImplementation"("org.testcontainers:testcontainers:$testcontainersVersion")
    "testImplementation"("org.testcontainers:junit-jupiter:$testcontainersVersion")
    "testImplementation"("org.testcontainers:gcloud:$testcontainersVersion")

    // Awaitility
    "testImplementation"("org.awaitility:awaitility-kotlin:$awaitilityVersion")
  }
}

/**
 * Configures data layer libs needed for interacting with the DB
 */
fun Project.dataLibs() {
  dependencies {
    "implementation"("org.jetbrains.exposed:exposed:$exposedVersion")
    "implementation"("org.postgresql:postgresql:$postgresqlVersion")
    "testImplementation"("org.testcontainers:postgresql:$testcontainersVersion")
  }
}

/**
 * Configures web layer libs needed for exposing REST APIs
 */
fun Project.webLibs() {
  dependencies {
    "implementation"("io.javalin:javalin:$javalinVersion")
  }
}