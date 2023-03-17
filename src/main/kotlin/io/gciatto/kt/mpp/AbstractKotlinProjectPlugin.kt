package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import java.util.Locale

@Suppress("TooManyFunctions")
abstract class AbstractKotlinProjectPlugin(targetName: String) : AbstractProjectPlugin() {

    companion object {
        private val SUPPORTED_TARGETS = setOf("jvm", "js", "multiplatform")
    }

    private val targetName: String = targetName.lowercase(Locale.getDefault()).also {
        require(it in SUPPORTED_TARGETS) {
            "Unsupported target: $it. Supported targets are: ${SUPPORTED_TARGETS.joinToString()}"
        }
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

    private fun DependencyScope.addMainDependencies(project: Project, target: String, skipBom: Boolean) {
        val kotlinStdlib = kotlin("stdlib-$target")
        api(kotlinStdlib)
        project.log("add api dependency to $kotlinStdlib")
        if (!skipBom) {
            val kotlinBom = kotlin("bom")
            implementation(kotlinBom)
            project.log("add implementation dependency to $kotlinBom")
        }
    }

    protected fun DependencyHandlerScope.addMainDependencies(
        project: Project,
        target: String,
        skipBom: Boolean = false
    ) = DependencyScope.of(this).addMainDependencies(project, target, skipBom)

    protected fun KotlinDependencyHandler.addMainDependencies(
        project: Project,
        target: String,
        skipBom: Boolean = false
    ) = DependencyScope.of(this).addMainDependencies(project, target, skipBom)

    private fun DependencyScope.addCommonTestDependencies(project: Project, target: String) {
        val testLib = kotlin("test-$target")
        test(testLib)
        project.log("add test dependency to $testLib")
        val annotationsLib = kotlin("test-annotations-$target")
        test(annotationsLib)
        project.log("add test dependency to $annotationsLib")
    }

    protected fun KotlinDependencyHandler.addCommonTestDependencies(project: Project, target: String = "common") {
        DependencyScope.of(this).addCommonTestDependencies(project, target)
    }

    private fun DependencyScope.addTestDependencies(project: Project, target: String, skipAnnotations: Boolean) {
        val testLib = kotlin("test-$target")
        test(testLib)
        project.log("add test dependency to $testLib")
        if (!skipAnnotations) {
            val annotationsLib = kotlin("test-annotations-$target")
            test(annotationsLib)
            project.log("add test dependency to $annotationsLib")
        }
    }

    protected fun KotlinDependencyHandler.addTestDependencies(
        project: Project,
        target: String = targetName,
        skipAnnotations: Boolean = false
    ) = DependencyScope.of(this).addTestDependencies(project, target, skipAnnotations)

    protected fun DependencyHandlerScope.addTestDependencies(
        project: Project,
        target: String = targetName,
        skipAnnotations: Boolean = false
    ) = DependencyScope.of(this).addTestDependencies(project, target, skipAnnotations)

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
