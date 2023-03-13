package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class KotlinJsOnly : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply(plugin = "org.jetbrains.kotlin.js")
        configure<KotlinJsProjectExtension> {
            js {
                useCommonJs()
                compilations.all { compilation ->
                    compilation.kotlinOptions {
                        main = "noCall"
                        freeCompilerArgs = (freeCompilerArgs + getProperty("ktCompilerArgs").split(";"))
                            .filter { it.isBlank() }
                    }
                }
                nodejs {
                    testTask {
                        useMocha {
                            timeout = getProperty("mochaTimeout")
                        }
                    }
                }
            }
        }
        dependencies {
            add("implementation", kotlin("bom"))
            add("api", kotlin("stdlib-js"))
        }
        tasks.register("jsTest") {
            it.group = "verification"
            it.dependsOn("test")
        }
        tasks.register("jsMainClasses") {
            it.group = "build"
            it.dependsOn("mainClasses")
        }
        tasks.register("jsTestClasses") {
            it.group = "build"
            it.dependsOn("testClasses")
        }
    }
}
