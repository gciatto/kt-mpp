package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class KotlinMultiplatform : AbstractKotlinProjectPlugin("multiplatform") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        val ktTargetJvmDisable = getBooleanProperty("ktTargetJvmDisable")
        val ktTargetJsDisable = getBooleanProperty("ktTargetJsDisable")
        configure<KotlinMultiplatformExtension> {
            if (ktTargetJvmDisable) {
                log("disable JVM target", LogLevel.WARN)
            } else {
                jvm { configureJvm() }
            }
            if (ktTargetJsDisable) {
                log("disable JS target", LogLevel.WARN)
            } else {
                js { configureJs() }
            }
            dependenciesFor("commonMain") {
                addMainDependencies("common")
            }
            dependenciesFor("commonTest") {
                addCommonTestDependencies()
            }
        }
    }

    private fun KotlinMultiplatformExtension.dependenciesFor(
        sourceSet: String,
        action: KotlinDependencyHandler.() -> Unit
    ) = sourceSets.getByName(sourceSet).dependencies(action)

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJvmTarget.configureJvm() {
        withJava()
        log("configure Kotlin JVM target to accept Java sources")
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureKotlinOptions()
                configureJvmKotlinOptions()
            }
        }
        dependenciesFor("jvmMain") {
            addMainDependencies("jdk8", skipBom = true)
        }
        dependenciesFor("jvmTest") {
            addTestDependencies("junit", skipAnnotations = true)
        }
    }

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJsTargetDsl.configureJs() {
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureKotlinOptions()
                configureJsKotlinOptions()
            }
        }
        configureNodeJs()
        dependenciesFor("jsMain") {
            addMainDependencies("js", skipBom = true)
        }
        dependenciesFor("jsTest") {
            addTestDependencies("js", skipAnnotations = true)
        }
    }
}
