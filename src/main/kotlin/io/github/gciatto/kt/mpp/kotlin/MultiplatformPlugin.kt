package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class MultiplatformPlugin : AbstractKotlinProjectPlugin("multiplatform") {
    override val relevantPublications: Set<String> =
        setOf("jvm", "js", "kotlinMultiplatform")

    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        multiPlatformHelper.initializeVersionsRelatedProperties()
        multiPlatformHelper.initializeKotlinRelatedProperties()
        configureKotlinVersionFromCatalogIfPossible()
        configureJvmVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        val ktTargetJvmDisable = multiPlatformHelper.ktTargetJvmDisable.orNull ?: false
        val ktTargetJsDisable = multiPlatformHelper.ktTargetJsDisable.orNull ?: false
        configure(KotlinMultiplatformExtension::class) {
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
                val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
                addMainDependencies(project, "common", skipBom = !useBom)
            }
            dependenciesFor("commonTest") {
                addTestDependencies(project, "common", skipAnnotations = false)
            }
            targets.all { target ->
                target.compilations.all { compilation ->
                    compilation.kotlinOptions {
                        configureKotlinOptions(target.targetCompilationId(compilation))
                    }
                }
            }
        }
    }

    private fun KotlinMultiplatformExtension.dependenciesFor(
        sourceSet: String,
        action: KotlinDependencyHandler.() -> Unit,
    ) = sourceSets.getByName(sourceSet).dependencies(action)

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJvmTarget.configureJvm() {
        multiPlatformHelper.initializeJvmRelatedProperties()
        withJava()
        log("configure Kotlin JVM target to accept Java sources")
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJvmKotlinOptions(targetCompilationId(compilation))
            }
        }
        val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
        dependenciesFor("jvmMain") {
            addMainDependencies(project, "jdk8", skipBom = !useBom)
        }
        dependenciesFor("jvmTest") {
            addTestDependencies(project, "junit", skipAnnotations = true)
        }
        addMultiplatformTaskAliases("jvm")
    }

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJsTargetDsl.configureJs() {
        multiPlatformHelper.initializeJsRelatedProperties()
        binaries.configureAutomatically()
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJsKotlinOptions(targetCompilationId(compilation))
            }
        }
        configureNodeJs()
        val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
        dependenciesFor("jsMain") {
            addMainDependencies(project, "js", skipBom = !useBom)
        }
        dependenciesFor("jsTest") {
            addTestDependencies(project, "js", skipAnnotations = true)
        }
        addMultiplatformTaskAliases("js")
    }
}
