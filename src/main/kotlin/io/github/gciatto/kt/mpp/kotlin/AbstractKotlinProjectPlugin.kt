package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.utils.jvmVersion
import io.github.gciatto.kt.mpp.utils.kotlinVersion
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import io.github.gciatto.kt.mpp.utils.nodeVersion
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import java.util.*

@Suppress("TooManyFunctions")
abstract class AbstractKotlinProjectPlugin(targetName: String) : AbstractProjectPlugin() {

    companion object {
        private const val PUBLICATION_TASK_NAME_PATTERN = "([A-Z]\\w+?)(?:Publication)?To([A-Z]\\w+)"
        private val pubTaskNamePattern = "^(publish|upload)$PUBLICATION_TASK_NAME_PATTERN$".toRegex()
    }

    private val targetName: String = targetName.lowercase(Locale.getDefault()).also {
        require(it in SUPPORTED_KOTLIN_TARGETS) {
            "Unsupported target: $it. Supported targets are: ${SUPPORTED_KOTLIN_TARGETS.joinToString()}"
        }
    }

    final override fun apply(target: Project) {
        super.apply(target)
        target.plugins.withType(MavenPublishPlugin::class.java) {
            target.configure(PublishingExtension::class) {
                target.run { customizePublishing() }
            }
        }
    }

    protected abstract val relevantPublications: Set<String>

    private fun String.declined() =
        capital().let { it + if (it.endsWith("h")) "es" else "s" }

    context (Project)
    private fun PublishingExtension.customizePublishing() {
        publications.withType(MavenPublication::class.java).matching { it.name in relevantPublications }.all { pub ->
            log("configuring relevant publication ${pub.name}")
            tasks.withType(AbstractPublishToMaven::class.java).all { task ->
                pubTaskNamePattern.matchEntire(task.name)?.let {
                    if (it.groupValues[2].equals(pub.name, ignoreCase = true)) {
                        val umbrellaTask = tasks.maybeCreate("${it.groupValues[1]}ProjectTo${it.groupValues[3]}")
                        umbrellaTask.group = "Publishing"
                        val description = "${it.groupValues[1].declined()} the whole project to ${it.groupValues[3]} " +
                            "via tasks: ${task.name}"
                        umbrellaTask.description = umbrellaTask.description
                            ?.let { desc -> desc + ", ${task.name}" }
                            ?: description
                        umbrellaTask.dependsOn(task)
                        log("let task ${umbrellaTask.path} depend on ${task.path}")
                    }
                }
            }
        }
        fixPublishing()
    }

    context (Project)
    protected open fun PublishingExtension.fixPublishing() {
        // does nothing be default
    }

    protected fun Project.configureKotlinVersionFromCatalogIfPossible() {
        kotlinVersion(multiPlatformHelper.kotlinVersion)
    }

    protected fun Project.configureJvmVersionFromCatalogIfPossible() {
        jvmVersion(multiPlatformHelper.jvmVersion)
    }

    protected fun Project.configureNodeVersionFromCatalogIfPossible() {
        nodeVersion(multiPlatformHelper.nodeVersion)
    }

    protected fun kotlinPlugin(name: String = targetName) =
        io.github.gciatto.kt.mpp.utils.kotlinPlugin(name)

    context(Project)
    protected fun KotlinJvmCompilerOptions.configureJvmKotlinOptions() {
        multiPlatformHelper.ktCompilerArgs.all {
            freeCompilerArgs.add(it)
            log("add JVM-specific free compiler arg for Kotlin compiler: $it")
        }
    }

    context(Project)
    protected fun KotlinJsCompilerOptions.configureJsKotlinOptions() {
        main.set(JsMainFunctionExecutionMode.NO_CALL)
        multiPlatformHelper.ktCompilerArgs.all {
            freeCompilerArgs.add(it)
            log("add JVM-specific free compiler arg for Kotlin compiler: $it")
        }
        multiPlatformHelper.ktCompilerArgsJs.all {
            freeCompilerArgs.add(it)
            log("add JS-specific free compiler arg for Kotlin compiler: $it")
        }
    }

    context(Project)
    protected fun KotlinCommonCompilerOptions.configureKotlinOptions() {
        allWarningsAsErrors.set(
            multiPlatformHelper.allWarningsAsErrors.map {
                if (it) {
                    log("consider all warnings as errors when compiling Kotlin sources")
                }
                it
            },
        )
        multiPlatformHelper.ktCompilerArgs.all {
            freeCompilerArgs.add(it)
            log("add free compiler arg for Kotlin compiler")
        }
    }

    private fun Any.toDependencyNotation(): String = when (this) {
        is Dependency -> listOfNotNull(group, name, version).joinToString(":")
        else -> toString()
    }

    private fun DependencyScope.addMainDependencies(project: Project, target: String, skipBom: Boolean) {
        val kotlinStdlib = kotlin("stdlib-$target")
        api(kotlinStdlib)
        project.log("add api dependency to ${kotlinStdlib.toDependencyNotation()}")
        if (!skipBom) {
            val kotlinBom = kotlin("bom")
            implementation(kotlinBom)
            project.log("add implementation dependency to ${kotlinBom.toDependencyNotation()}")
        }
    }

    protected fun DependencyHandlerScope.addMainDependencies(
        project: Project,
        target: String,
        skipBom: Boolean = false,
    ) = DependencyScope.of(this).addMainDependencies(project, target, skipBom)

    protected fun KotlinDependencyHandler.addMainDependencies(
        project: Project,
        target: String,
        skipBom: Boolean = false,
    ) = DependencyScope.of(this).addMainDependencies(project, target, skipBom)

    private fun DependencyScope.addTestDependencies(project: Project, target: String, skipAnnotations: Boolean) {
        val testLib = kotlin("test-$target")
        test(testLib)
        project.log("add test dependency to ${testLib.toDependencyNotation()}")
        if (!skipAnnotations) {
            val annotationsLib = kotlin("test-annotations-$target")
            test(annotationsLib)
            project.log("add test dependency to ${annotationsLib.toDependencyNotation()}")
        }
    }

    protected fun KotlinDependencyHandler.addTestDependencies(
        project: Project,
        target: String = targetName,
        skipAnnotations: Boolean = false,
    ) = DependencyScope.of(this).addTestDependencies(project, target, skipAnnotations)

    protected fun DependencyHandlerScope.addTestDependencies(
        project: Project,
        target: String = targetName,
        skipAnnotations: Boolean = false,
    ) = DependencyScope.of(this).addTestDependencies(project, target, skipAnnotations)

    protected fun Project.addPlatformSpecificTaskAliases() {
        tasks.create("${targetName}Test") {
            it.group = "verification"
            it.dependsOn("test")
            log("add ${it.path} task as an alias for ${it.sibling("test")}")
        }
        tasks.create("${targetName}MainClasses") {
            it.group = "build"
            it.dependsOn("mainClasses")
            log("add ${it.path} task as an alias for ${it.sibling("mainClasses")}")
        }
        tasks.create("${targetName}TestClasses") {
            it.group = "build"
            it.dependsOn("testClasses")
            log("add ${it.path} task as an alias for ${it.sibling("testClasses")}")
        }
    }

    protected fun Project.addMultiplatformTaskAliases(target: String) {
        tasks.maybeCreate("test").let {
            it.dependsOn("${target}Test")
            log("let task ${it.path} be triggered by ${it.sibling("test")}")
        }
    }

    protected fun KotlinTarget.targetCompilationId(compilation: KotlinCompilation<*>): String =
        "${name}${compilation.compilationName.capital()}"

    protected fun targetCompilationId(task: Task): String =
        task.name.replace("compile", "")

    context (Project, KotlinJsTargetDsl)
    protected fun configureNodeJs() {
        nodejs {
            log("configure kotlin JS to target NodeJS")
            testTask(
                Action {
                    it.useMocha {
                        log("use Mocha as JS test framework")
                        timeout = project.multiPlatformHelper.mochaTimeout.orNull ?: timeout
                        log("set Mocha per-test-case timeout to $timeout")
                    }
                },
            )
        }
    }

    context(Project)
    protected fun KotlinJsBinaryContainer.configureAutomatically() {
        when (multiPlatformHelper.jsBinaryType.orNull) {
            JsBinaryType.LIBRARY -> {
                library()
                log("configure kotlin js to produce a library")
            }
            JsBinaryType.EXECUTABLE -> {
                executable()
                log("configure kotlin js to produce an executable")
            }
            else -> {}
        }
    }
}
