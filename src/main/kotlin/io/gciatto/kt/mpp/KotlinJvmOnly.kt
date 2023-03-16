package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinJvmOnly : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        log("apply org.jetbrains.kotlin.jvm plugin")
        apply(plugin = "java-library")
        log("apply java-library plugin")
        tasks.withType(KotlinCompile::class.java) { task ->
            task.kotlinOptions {
                allWarningsAsErrors = true
                val ktCompilerArgs = getProperty("ktCompilerArgs").split(";").filter { it.isNotBlank() }
                if (ktCompilerArgs.isNotEmpty()) {
                    freeCompilerArgs += ktCompilerArgs
                    log("add free compiler args: ${ktCompilerArgs.joinToString()}")
                }
                val ktCompilerArgsJvm = getProperty("ktCompilerArgsJvm").split(";").filter { it.isNotBlank() }
                if (ktCompilerArgsJvm.isNotEmpty()) {
                    freeCompilerArgs += ktCompilerArgsJvm
                    log("add JVM-specific free compiler args: ${ktCompilerArgs.joinToString()}")
                }
            }
        }
        dependencies {
            val kotlinStdlib = kotlin("stdlib-jdk8")
            add("api", kotlinStdlib)
            log("add api dependency to $kotlinStdlib")
            val kotlinBom = kotlin("bom")
            add("implementation", kotlinBom)
            log("add implementation dependency to $kotlinBom")
        }
        configure<JavaPluginExtension> {
            withSourcesJar()
            log("configure JVM library to include sources JAR")
        }
        tasks.register("jvmTest") {
            it.group = "verification"
            it.dependsOn("test")
            log("add ${it.path} task as an alias for ${it.sibling("test")}")
        }
        tasks.register("jvmMainClasses") {
            it.group = "build"
            it.dependsOn("mainClasses")
            log("add ${it.path} task as an alias for ${it.sibling("mainClasses")}")
        }
        tasks.register("jvmTestClasses") {
            it.group = "build"
            it.dependsOn("testClasses")
            log("add ${it.path} task as an alias for ${it.sibling("testClasses")}")
        }
    }
}
