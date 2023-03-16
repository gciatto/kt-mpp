package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import java.util.Locale

@Suppress("TooManyFunctions")
abstract class AbstractKotlinProjectPlugin(targetName: String) : AbstractProjectPlugin() {

    companion object {
        private val SUPPORTED_TARGETS = setOf("jvm", "js")
    }

    private val targetName: String = targetName.lowercase(Locale.getDefault()).also {
        require(it in SUPPORTED_TARGETS)
    }

    protected fun kotlinPlugin(name: String = targetName) =
        "org.jetbrains.kotlin.$name"

    context(Project)
    protected fun KotlinJvmOptions.configureJvmKotlinOptions() {
        val ktCompilerArgsJvm = getProperty("ktCompilerArgsJvm").split(";").filter { it.isNotBlank() }
        if (ktCompilerArgsJvm.isNotEmpty()) {
            freeCompilerArgs += ktCompilerArgsJvm
            log("add JVM-specific free compiler args: ${ktCompilerArgsJvm.joinToString()}")
        }
    }

    context(Project)
    protected fun KotlinJsOptions.configureJsKotlinOptions() {
        main = "noCall"
        log("configure kotlin JS compiler to avoid calling main")
        val ktCompilerArgsJs = getProperty("ktCompilerArgsJs").split(";").filter { it.isNotBlank() }
        if (ktCompilerArgsJs.isNotEmpty()) {
            freeCompilerArgs += ktCompilerArgsJs
            log("add JS-specific free compiler args: ${ktCompilerArgsJs.joinToString()}")
        }
    }

    context(Project)
    protected fun KotlinCommonOptions.configureKotlinOptions() {
        allWarningsAsErrors = getBooleanProperty("allWarningsAsErrors", default = true)
        if (allWarningsAsErrors) {
            log("consider all warnings as errors when compiling Kotlin sources")
        }
        val ktCompilerArgs = getProperty("ktCompilerArgs").split(";").filter { it.isNotBlank() }
        if (ktCompilerArgs.isNotEmpty()) {
            freeCompilerArgs += ktCompilerArgs
            log("add free compiler args: ${ktCompilerArgs.joinToString()}")
        }
    }

    context(Project)
    private fun DependencyScope.addMainDependencies(target: String, skipBom: Boolean) {
        val kotlinStdlib = kotlin("stdlib-$target")
        api(kotlinStdlib)
        log("add api dependency to $kotlinStdlib")
        if (!skipBom) {
            val kotlinBom = kotlin("bom")
            implementation(kotlinBom)
            log("add implementation dependency to $kotlinBom")
        }
    }

    context(Project)
    protected fun DependencyHandlerScope.addMainDependencies(target: String, skipBom: Boolean = false) {
        DependencyScope.of(this).addMainDependencies(target, skipBom)
    }

    context(Project)
    protected fun KotlinDependencyHandler.addMainDependencies(target: String, skipBom: Boolean = false) {
        DependencyScope.of(this).addMainDependencies(target, skipBom)
    }

    context(Project)
    private fun DependencyScope.addCommonTestDependencies(target: String) {
        val testLib = kotlin("test-$target")
        test(testLib)
        log("add test dependency to $testLib")
        val annotationsLib = kotlin("test-annotations-$target")
        test(annotationsLib)
        log("add test dependency to $annotationsLib")
    }

    context(Project)
    protected fun KotlinDependencyHandler.addCommonTestDependencies(target: String = "common") {
        DependencyScope.of(this).addCommonTestDependencies(target)
    }

    context(Project)
    protected fun DependencyHandlerScope.addCommonTestDependencies(target: String = "common") {
        DependencyScope.of(this).addCommonTestDependencies(target)
    }

    context(Project)
    private fun DependencyScope.addTestDependencies(target: String, skipAnnotations: Boolean) {
        val testLib = kotlin("test-$target")
        test(testLib)
        log("add test dependency to $testLib")
        if (!skipAnnotations) {
            val annotationsLib = kotlin("test-annotations-$target")
            test(annotationsLib)
            log("add test dependency to $annotationsLib")
        }
    }

    context(Project)
    protected fun KotlinDependencyHandler.addTestDependencies(
        target: String = targetName,
        skipAnnotations: Boolean = false
    ) {
        DependencyScope.of(this).addTestDependencies(target, skipAnnotations)
    }

    context(Project)
    protected fun DependencyHandlerScope.addTestDependencies(
        target: String = targetName,
        skipAnnotations: Boolean = false
    ) {
        DependencyScope.of(this).addTestDependencies(target, skipAnnotations)
    }

    protected fun Project.addTaskAliases() {
        tasks.register("${targetName}Test") {
            it.group = "verification"
            it.dependsOn("test")
            log("add ${it.path} task as an alias for ${it.sibling("test")}")
        }
        tasks.register("${targetName}MainClasses") {
            it.group = "build"
            it.dependsOn("mainClasses")
            log("add ${it.path} task as an alias for ${it.sibling("mainClasses")}")
        }
        tasks.register("${targetName}TestClasses") {
            it.group = "build"
            it.dependsOn("testClasses")
            log("add ${it.path} task as an alias for ${it.sibling("testClasses")}")
        }
    }

    context (Project, KotlinJsTargetDsl)
    protected fun configureNodeJs() {
        nodejs {
            log("configure kotlin JS to target NodeJS")
            testTask {
                useMocha {
                    log("use Mocha as JS test framework")
                    timeout = getProperty("mochaTimeout")
                    log("set Mocha per-test-case timeout to $timeout")
                }
            }
        }
    }
}
