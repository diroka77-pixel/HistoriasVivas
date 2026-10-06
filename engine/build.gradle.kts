plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories { mavenCentral() }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
