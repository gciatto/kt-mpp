package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class JsOnlyPlugin : AbstractKotlinProjectPlugin("js") {
    override val relevantPublications: Set<String> = setOf("kotlinOSSRH")

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        multiPlatformHelper.initializeVersionsRelatedProperties(jvm = false)
        multiPlatformHelper.initializeKotlinRelatedProperties()
        multiPlatformHelper.initializeJsRelatedProperties()
        configureKotlinVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        configure(KotlinJsProjectExtension::class) {
            js {
                binaries.configureAutomatically()
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilerOptions {
                    configureKotlinOptions()
                    configureJsKotlinOptions()
                }
                configureNodeJs()
                this@configure.sourceSets.getByName("main") {
                    dependencies {
                        val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
                        addMainDependencies(project, target = "js", skipBom = !useBom)
                    }
                }
                this@configure.sourceSets.getByName("test") {
                    dependencies {
                        addTestDependencies(project, target = "js", skipAnnotations = true)
                    }
                }
            }
        }
        addPlatformSpecificTaskAliases()
    }
}
