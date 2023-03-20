package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

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
        }
        dependencies {
            addMainDependencies(project, target = "js")
            addTestDependencies(project, target = "js")
        }
        addTaskAliases()
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
