package io.github.gciatto.kt.mpp

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
        configureKotlinVersionFromCatalogIfPossible()
        configureJvmVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        val ktTargetJvmDisable = getBooleanProperty("ktTargetJvmDisable")
        val ktTargetJsDisable = getBooleanProperty("ktTargetJsDisable")
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
                addMainDependencies(project, "common", skipBom = false)
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
        action: KotlinDependencyHandler.() -> Unit
    ) = sourceSets.getByName(sourceSet).dependencies(action)

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJvmTarget.configureJvm() {
        withJava()
        log("configure Kotlin JVM target to accept Java sources")
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJvmKotlinOptions(targetCompilationId(compilation))
            }
        }
        dependenciesFor("jvmMain") {
            addMainDependencies(project, "jdk8", skipBom = true)
        }
        dependenciesFor("jvmTest") {
            addTestDependencies(project, "junit", skipAnnotations = true)
        }
        addMultiplatformTaskAliases("jvm")
    }

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJsTargetDsl.configureJs() {
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJsKotlinOptions(targetCompilationId(compilation))
            }
        }
        configureNodeJs()
        dependenciesFor("jsMain") {
            addMainDependencies(project, "js", skipBom = true)
        }
        dependenciesFor("jsTest") {
            addTestDependencies(project, "js", skipAnnotations = true)
        }
        addMultiplatformTaskAliases("js")
    }

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(allWarningsAsErrors)
        addProperty(ktCompilerArgs)
        addProperty(ktCompilerArgsJvm)
        addProperty(ktCompilerArgsJs)
        addProperty(mochaTimeout)
        addProperty(ktTargetJvmDisable)
        addProperty(ktTargetJsDisable)
        addProperty(versionsFromCatalog)
        addProperty(nodeVersion)
    }
}
