import io.gitlab.arturbosch.detekt.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    val kotlinVersion = "2.1.21"
    val detektVersion = "1.23.8"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("plugin.power-assert") version kotlinVersion
    id("io.gitlab.arturbosch.detekt") version detektVersion
    application
}

repositories {
    mavenCentral()
}

dependencies {
    val wiremockVersion = "3.13.0"
    val wiremockStateExtensionVersion = "0.10.0"
    val ktorVersion = "3.1.3"
    val jacksonVersion = "2.18.4"
    val brotliVersion = "0.1.2"
    val kotlinLoggingVersion = "7.0.7"
    val logbackVersion = "1.5.18"
    val logstashEncoder = "8.1"
    val detektVersion = "1.23.8"
    val junitVersion = "5.12.2"
    val assertjVersion = "3.27.3"

    implementation("org.wiremock:wiremock:$wiremockVersion")
    implementation("org.wiremock.extensions:wiremock-state-extension:$wiremockStateExtensionVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.brotli:dec:$brotliVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashEncoder")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-java:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-xml:$ktorVersion")
}

val javaVersion = JavaVersion.VERSION_21

java {
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
}

fun listLibsFiles() = configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }

val copyLibs by tasks.registering(Copy::class) {
    dependsOn(configurations.runtimeClasspath)
    from(listLibsFiles())
    into("${layout.buildDirectory.get()}/libs/libs")
}

val appMainClass = "me.velikiy.frozenflow.MainKt"

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
    }
}

application {
    mainClass.set(appMainClass)
}

detekt {
    buildUponDefaultConfig = true
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertContains",
        "kotlin.test.assertContentEquals",
        "kotlin.test.assertEquals",
        "kotlin.test.assertFalse",
        "kotlin.test.assertIs",
        "kotlin.test.assertIsNot",
        "kotlin.test.assertNotEquals",
        "kotlin.test.assertNotNull",
        "kotlin.test.assertNotSame",
        "kotlin.test.assertNull",
        "kotlin.test.assertSame",
        "kotlin.test.assertTrue",
    )
    includedSourceSets.addAll("test")
}

tasks.jar {
    dependsOn(copyLibs)
    manifest {
        attributes(
            mapOf(
                "Class-Path" to listLibsFiles().map { it.name }.joinToString(" ") {
                    "libs/$it"
                },
                "Main-Class" to appMainClass
            )
        )
    }
}

tasks.withType<Detekt> {
    jvmTarget = javaVersion.toString()
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
