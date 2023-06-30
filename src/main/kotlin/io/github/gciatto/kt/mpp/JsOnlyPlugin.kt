package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class JsOnlyPlugin : AbstractKotlinProjectPlugin("js") {
    override val relevantPublications: Set<String> = setOf("kotlinOSSRH")

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
                sourceSets.getByName("main") {
                    dependencies {
                        addMainDependencies(project, target = "js", skipBom = !getBooleanProperty("useKotlinBom"))
                    }
                }
                sourceSets.getByName("test") {
                    dependencies {
                        addTestDependencies(project, target = "js", skipAnnotations = true)
                    }
                }
            }
        }
        addPlatformSpecificTaskAliases()
    }

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(allWarningsAsErrors)
        addProperty(ktCompilerArgs)
        addProperty(ktCompilerArgsJs)
        addProperty(mochaTimeout)
        addProperty(versionsFromCatalog)
        addProperty(nodeVersion)
        addProperty(useKotlinBom)
    }
}
