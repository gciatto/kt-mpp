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
                configureJvmTarget()
            }
            if (ktTargetJsDisable) {
                log("disable JS target", LogLevel.WARN)
            } else {
                configureJsTarget()
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

    @Suppress("DEPRECATION")
    context(p: Project)
    private fun KotlinMultiplatformExtension.configureJvmTarget() {
        jvm {
            p.multiPlatformHelper.initializeJvmRelatedProperties()
            p.log("configure Kotlin JVM target to accept Java sources")
            this.compilerOptions {
                configureJvmKotlinOptions()
            }
            this@configureJvmTarget.dependenciesFor("jvmMain") {
                val useBom = p.multiPlatformHelper.useKotlinBom.orNull ?: false
                addMainDependencies(p.project, "jdk8", skipBom = !useBom)
            }
            this@configureJvmTarget.dependenciesFor("jvmTest") {
                addTestDependencies(p.project, "junit", skipAnnotations = true)
            }
            p.addMultiplatformTaskAliases("jvm")
        }
    }
}
