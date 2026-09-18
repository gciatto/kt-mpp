package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

class JsOnlyPlugin : AbstractKotlinProjectPlugin("multiplatform") {
    override val relevantPublications: Set<String> = setOf("kotlinOSSRH")

    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        multiPlatformHelper.initializeVersionsRelatedProperties(jvm = false)
        multiPlatformHelper.initializeKotlinRelatedProperties()
        multiPlatformHelper.initializeJsRelatedProperties()
        multiPlatformHelper.ktTargetJvmDisable.set(true)
        configureKotlinVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        configure(KotlinMultiplatformExtension::class) {
            configureJsTarget()
            compilerOptions {
                configureKotlinOptions()
            }
        }
    }
}
