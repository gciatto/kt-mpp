package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

class KotlinJsOnly : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply(plugin = "org.jetbrains.kotlin.js")
        log("apply org.jetbrains.kotlin.js plugin")
        configure<KotlinJsProjectExtension> {
            js {
                useCommonJs()
                log("configure kotlin js to use CommonJS")
                compilations.all { compilation ->
                    compilation.kotlinOptions {
                        main = "noCall"
                        log("configure kotlin JS compiler to avoid calling main")
                        log("apply org.jetbrains.kotlin.js plugin")
                        val ktCompilerArgs = getProperty("ktCompilerArgs").split(";").filter { it.isNotBlank() }
                        if (ktCompilerArgs.isNotEmpty()) {
                            freeCompilerArgs += ktCompilerArgs
                            log("add free compiler args: ${ktCompilerArgs.joinToString()}")
                        }
                        val ktCompilerArgsJs = getProperty("ktCompilerArgsJs").split(";").filter { it.isNotBlank() }
                        if (ktCompilerArgsJs.isNotEmpty()) {
                            freeCompilerArgs += ktCompilerArgsJs
                            log("add JS-specific free compiler args: ${ktCompilerArgs.joinToString()}")
                        }
                    }
                }
                nodejs {
                    testTask {
                        useMocha {
                            log("use mocha as JS test framework")
                            timeout = getProperty("mochaTimeout")
                            log("set mocha per-test-case timeout to $timeout")
                        }
                    }
                }
            }
        }
        dependencies {
            val kotlinStdlib = kotlin("stdlib-js")
            add("api", kotlinStdlib)
            log("add api dependency to $kotlinStdlib")
            val kotlinBom = kotlin("bom")
            add("implementation", kotlinBom)
            log("add implementation dependency to $kotlinBom")
        }
        tasks.register("jsTest") {
            it.group = "verification"
            it.dependsOn("test")
            log("add ${it.path} task as an alias for ${it.sibling("test")}")
        }
        tasks.register("jsMainClasses") {
            it.group = "build"
            it.dependsOn("mainClasses")
            log("add ${it.path} task as an alias for ${it.sibling("mainClasses")}")
        }
        tasks.register("jsTestClasses") {
            it.group = "build"
            it.dependsOn("testClasses")
            log("add ${it.path} task as an alias for ${it.sibling("testClasses")}")
        }
    }
}
