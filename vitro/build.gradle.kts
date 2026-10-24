import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

android {
    namespace = "io.github.itshedimisawi.vitro"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    explicitApi()
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}

val abiRuntime = configurations.create("abiRuntime") {
    isCanBeConsumed = false
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)

    testImplementation(libs.junit)

    abiRuntime(libs.abi.asm)
    abiRuntime(libs.abi.asm.tree)
    abiRuntime(libs.abi.kotlin.metadata)
}

// The validator only wires itself to the standalone Kotlin plugins, so its tasks are registered
// here against the release classes that the Android plugin's built-in Kotlin compiles.
val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
    inputClassesDirs.from(tasks.named<KotlinJvmCompile>("compileReleaseKotlin").flatMap { it.destinationDirectory })
    outputApiFile.set(layout.buildDirectory.file("api/vitro.api"))
    runtimeClasspath.from(abiRuntime)
}

tasks.register<Copy>("apiDump") {
    group = "verification"
    description = "Writes the public API of the library to api/vitro.api."
    from(apiBuild.flatMap { it.outputApiFile })
    into(layout.projectDirectory.dir("api"))
}

val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
    group = "verification"
    description = "Checks that the public API of the library matches api/vitro.api."
    projectApiFile.set(layout.projectDirectory.file("api/vitro.api"))
    generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
}

tasks.named("check") { dependsOn(apiCheck) }
