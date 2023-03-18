package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class JsOnlyPlugin : AbstractKotlinProjectPlugin("js") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        configure(KotlinJsProjectExtension::class) {
            js {
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilations.all { compilation ->
                    compilation.kotlinOptions {
                        configureKotlinOptions(compilation.compilationName)
                        configureJsKotlinOptions(compilation.compilationName)
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
}
