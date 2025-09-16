package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class JsOnlyPlugin : AbstractKotlinProjectPlugin("multiplatform") {
    override val relevantPublications: Set<String> = setOf("kotlinOSSRH")

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
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
            js {
                multiPlatformHelper.initializeJsRelatedProperties()
                binaries.configureAutomatically()
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilerOptions {
                    configureKotlinOptions()
                    configureJsKotlinOptions()
                }
                configureNodeJs()
                this@configure.sourceSets.named("jsMain").configure { x ->
                    x.dependencies { d ->
                        val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
                        d.addMainDependencies(project, target = "js", skipBom = !useBom)
                    }
                }
                this@configure.sourceSets.named("jsTest").configure { x ->
                    x.dependencies { d ->
                        d.addTestDependencies(project, target = "js", skipAnnotations = true)
                    }
                }
                addMultiplatformTaskAliases("js")
//                binaries.configureAutomatically()
//                useCommonJs()
//                log("configure kotlin js to use CommonJS")
//                compilerOptions {
//                    configureKotlinOptions()
//                    configureJsKotlinOptions()
//                }
//                configureNodeJs()
//                this@configure.sourceSets.getByName("main") {
//                    dependencies {
//                        val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
//                        addMainDependencies(project, target = "js", skipBom = !useBom)
//                    }
//                }
//                this@configure.sourceSets.getByName("test") {
//                    dependencies {
//                        addTestDependencies(project, target = "js", skipAnnotations = true)
//                    }
//                }
            }
        }
        // addPlatformSpecificTaskAliases()
    }
}
