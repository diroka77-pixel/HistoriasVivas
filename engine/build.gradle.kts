plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}
repositories { mavenCentral() }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
