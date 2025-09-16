package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

@OptIn(ExperimentalKotlinGradlePluginApi::class)
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
                jvm { x -> x.configureJvm() }
            }
            if (ktTargetJsDisable) {
                log("disable JS target", LogLevel.WARN)
            } else {
                js { x -> x.configureJs() }
            }
            dependenciesFor("commonMain") {
                val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
                addMainDependencies(project, "common", skipBom = !useBom)
            }
            dependenciesFor("commonTest") {
                addTestDependencies(project, "common", skipAnnotations = false)
            }
            compilerOptions {
                configureKotlinOptions()
            }
        }
    }

    // context(p : Project, k : KotlinMultiplatformExtension)
    private fun KotlinMultiplatformExtension.dependenciesFor(
        sourceSet: String,
        action: KotlinDependencyHandler.() -> Unit,
    ) = sourceSets.named(sourceSet).dependencies(action)

    @Suppress("DEPRECATION")
    context(p: Project, k: KotlinMultiplatformExtension)
    private fun KotlinJvmTarget.configureJvm() {
        p.multiPlatformHelper.initializeJvmRelatedProperties()
        p.log("configure Kotlin JVM target to accept Java sources")
        this.compilerOptions {
            configureJvmKotlinOptions()
        }
        val useBom = p.multiPlatformHelper.useKotlinBom.orNull ?: false
        k.dependenciesFor("jvmMain") {
            addMainDependencies(p.project, "jdk8", skipBom = !useBom)
        }
        k.dependenciesFor("jvmTest") {
            addTestDependencies(p.project, "junit", skipAnnotations = true)
        }
        p.addMultiplatformTaskAliases("jvm")
    }

    context(p: Project, k: KotlinMultiplatformExtension)
    private fun KotlinJsTargetDsl.configureJs() {
        p.multiPlatformHelper.initializeJsRelatedProperties()
        this.binaries.configureAutomatically()
        this.compilerOptions {
            configureJsKotlinOptions()
        }
        with(this) {
            configureNodeJs()
        }
        val useBom = p.multiPlatformHelper.useKotlinBom.orNull ?: false
        k.dependenciesFor("jsMain") {
            addMainDependencies(p.project, "js", skipBom = !useBom)
        }
        k.dependenciesFor("jsTest") {
            addTestDependencies(p.project, "js", skipAnnotations = true)
        }
        p.addMultiplatformTaskAliases("js")
    }
}
