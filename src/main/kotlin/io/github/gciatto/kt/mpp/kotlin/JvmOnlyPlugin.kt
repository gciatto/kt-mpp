package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class JvmOnlyPlugin : AbstractKotlinProjectPlugin("jvm") {
    override val relevantPublications: Set<String> = setOf("kotlinOSSRH", "javaOSSRH")

    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        apply(plugin = "java-library")
        log("apply java-library plugin")
        multiPlatformHelper.initializeVersionsRelatedProperties(node = false)
        multiPlatformHelper.initializeKotlinRelatedProperties()
        multiPlatformHelper.initializeJvmRelatedProperties()
        configureKotlinVersionFromCatalogIfPossible()
        configureJvmVersionFromCatalogIfPossible()
        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                configureKotlinOptions()
                configureJvmKotlinOptions()
            }
        }
        tasks.getByName("javadoc") {
            it.enabled = !(multiPlatformHelper.disableJavadocTask.orNull ?: false)
        }
        dependencies {
            val useBom = multiPlatformHelper.useKotlinBom.orNull ?: false
            addMainDependencies(project, target = "jdk8", skipBom = !useBom)
            addTestDependencies(project, target = "junit", skipAnnotations = true)
        }
        configure(JavaPluginExtension::class) {
            withSourcesJar()
            log("configure JVM library to include sources JAR")
        }
        addPlatformSpecificTaskAliases()
    }
}
