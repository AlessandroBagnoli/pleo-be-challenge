import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

private const val coroutinesVersion = "1.6.4"
private const val googleCloudPubsub = "1.123.5"
private const val junitVersion = "5.9.2"
private const val testcontainersVersion = "1.17.6"
private const val awaitilityVersion = "4.2.0"
private const val slf4jSimpleVersion = "2.0.6"
private const val kotlinLoggingVersion = "3.0.5"
private const val mockkVersion = "1.13.4"

private const val exposeVersion = "0.17.14"
private const val sqliteJdbcVersion = "3.41.0.0"

private const val javalinVersion = "5.4.2"
private const val jacksonDatabindVersion = "2.14.2"

/**
 * Configures the current project as a Kotlin project by adding the Kotlin `stdlib` as a dependency.
 */
fun Project.kotlinProject() {
  dependencies {
    // Kotlin libs
    "implementation"(kotlin("stdlib"))

    // Coroutines
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    //PubSub
    "implementation"("com.google.cloud:google-cloud-pubsub:$googleCloudPubsub")

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
    "testImplementation"("org.awaitility:awaitility-kotlin:4.2.0")
  }
}

/**
 * Configures data layer libs needed for interacting with the DB
 */
fun Project.dataLibs() {
  dependencies {
    "implementation"("org.jetbrains.exposed:exposed:$exposeVersion")
    "implementation"("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
  }
}

/**
 * Configures web layer libs needed for exposing REST APIs
 */
fun Project.webLibs() {
  dependencies {
    "implementation"("io.javalin:javalin:$javalinVersion")
    "implementation"("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
  }
}