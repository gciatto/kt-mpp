package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class JsOnlyPlugin : AbstractKotlinProjectPlugin("js") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        configureKotlinVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        configure(KotlinJsProjectExtension::class) {
            js {
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilations.all { compilation ->
                    compilation.kotlinOptions {
                        configureKotlinOptions(targetCompilationId(compilation))
                        configureJsKotlinOptions(targetCompilationId(compilation))
                    }
                }
                configureNodeJs()
            }
            sourceSets.getByName("main") { addMissingSourcesJar(it) }
        }
        dependencies {
            addMainDependencies(project, target = "js")
            addTestDependencies(project, target = "js", skipAnnotations = true)
        }
        addPlatformSpecificTaskAliases()
    }

    private fun Project.addMissingSourcesJar(sourceSet: KotlinSourceSet) {
        createJarTask("sourcesJar", "sources", "build") {
            val srcDirs = sourceSet.kotlin.sourceDirectories + sourceSet.resources.sourceDirectories
            from(srcDirs)
            log("create task $path from directories: ${srcDirs.files.joinToString()}")
            addJarTaskToAllPublications(this)
        }
    }

    private fun Project.addJarTaskToAllPublications(task: Jar) {
        plugins.withId("maven-publish") {
            configure(PublishingExtension::class) {
                publications.withType(MavenPublication::class.java) { pub ->
                    pub.artifact(task)
                    log("add task ${task.path} to publication ${pub.name}, as sources artifact")
                }
            }
        }
    }

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(allWarningsAsErrors)
        addProperty(ktCompilerArgs)
        addProperty(ktCompilerArgsJs)
        addProperty(mochaTimeout)
        addProperty(versionsFromCatalog)
        addProperty(nodeVersion)
    }
}
