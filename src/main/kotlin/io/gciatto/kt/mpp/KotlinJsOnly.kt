package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class KotlinJsOnly : AbstractKotlinProjectPlugin("js") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        configure<KotlinJsProjectExtension> {
            js {
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilations.all { compilation ->
                    compilation.kotlinOptions {
                        configureKotlinOptions()
                        configureJsKotlinOptions()
                    }
                }
                configureNodeJs()
            }
        }
        dependencies {
            addMainDependencies(target = "js")
            addTestDependencies(target = "js")
        }
        addTaskAliases()
    }
}
