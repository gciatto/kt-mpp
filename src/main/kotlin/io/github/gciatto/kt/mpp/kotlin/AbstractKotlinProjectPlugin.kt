package io.github.gciatto.kt.mpp.kotlin

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.helpers.JsModuleSystem
import io.github.gciatto.kt.mpp.utils.jvmVersion
import io.github.gciatto.kt.mpp.utils.kotlinVersion
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.maybeRegister
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import io.github.gciatto.kt.mpp.utils.nodeVersion
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import java.util.Locale

@Suppress("TooManyFunctions")
abstract class AbstractKotlinProjectPlugin(
    targetName: String,
) : AbstractProjectPlugin() {
    private val targetName: String =
        targetName.lowercase(Locale.getDefault()).also {
            require(it in SUPPORTED_KOTLIN_TARGETS) {
                "Unsupported target: $it. Supported targets are: ${SUPPORTED_KOTLIN_TARGETS.joinToString()}"
            }
        }

    final override fun apply(target: Project) {
        super.apply(target)
    }

    protected abstract val relevantPublications: Set<String>

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
        io.github.gciatto.kt.mpp.utils
            .kotlinPlugin(name)

    context(p: Project)
    protected fun KotlinJvmCompilerOptions.configureJvmKotlinOptions() {
        p.multiPlatformHelper.ktCompilerArgs.all {
            freeCompilerArgs.add(it)
            p.log("add JVM-specific free compiler arg for Kotlin compiler: $it")
        }
    }

    context(p: Project)
    protected fun KotlinJsCompilerOptions.configureJsKotlinOptions() {
        p.multiPlatformHelper.jsMainFunctionExecutionMode.orNull?.let {
            main.set(it)
            p.log("set JS main function execution mode to $it")
        }
        p.multiPlatformHelper.ktCompilerArgsJs.all {
            freeCompilerArgs.add(it)
            p.log("add JS-specific free compiler arg for Kotlin compiler: $it")
        }
    }

    context(p: Project)
    protected fun KotlinCommonCompilerOptions.configureKotlinOptions() {
        allWarningsAsErrors.set(
            p.multiPlatformHelper.allWarningsAsErrors.map {
                if (it) {
                    p.log("consider all warnings as errors when compiling Kotlin sources")
                }
                it
            },
        )
        p.multiPlatformHelper.ktCompilerArgs.all {
            freeCompilerArgs.add(it)
            p.log("add free compiler arg for Kotlin compiler")
        }
    }

    private fun Any.toDependencyNotation(): String =
        when (this) {
            is Dependency -> listOfNotNull(group, name, version).joinToString(":")
            else -> toString()
        }

    private fun DependencyScope.addMainDependencies(
        project: Project,
        target: String,
        skipBom: Boolean,
    ) {
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

    private fun DependencyScope.addTestDependencies(
        project: Project,
        target: String,
        skipAnnotations: Boolean,
    ) {
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
        tasks.register("${targetName}Test") {
            it.group = "verification"
            it.dependsOn(tasks.named("test"))
            log("add ${it.path} task as an alias for ${it.sibling("test")}")
        }
        tasks.register("${targetName}MainClasses") {
            it.group = "build"
            it.dependsOn(tasks.named("mainClasses"))
            log("add ${it.path} task as an alias for ${it.sibling("mainClasses")}")
        }
        tasks.register("${targetName}TestClasses") {
            it.group = "build"
            it.dependsOn(tasks.named("testClasses"))
            log("add ${it.path} task as an alias for ${it.sibling("testClasses")}")
        }
    }

    protected fun Project.addMultiplatformTaskAliases(target: String) {
        maybeRegister<Task>("test") {
            this.dependsOn(tasks.named("${target}Test"))
            log("let task test be triggered by ${this.sibling("test")}")
        }
    }

    protected fun KotlinMultiplatformExtension.dependenciesFor(
        sourceSet: String,
        action: KotlinDependencyHandler.() -> Unit,
    ) = sourceSets.named(sourceSet).dependencies(action)

    context(p: Project)
    protected fun KotlinMultiplatformExtension.configureJsTarget() {
        js {
            p.multiPlatformHelper.initializeJsRelatedProperties()
            if (p.multiPlatformHelper.jsTargetBrowser.get()) {
                configureJsForBrowser()
            }
            binaries.configureAutomatically()
            configureJsModuleSystem()
            compilerOptions {
                configureKotlinOptions()
                configureJsKotlinOptions()
            }
            if (p.multiPlatformHelper.jsTargetNode.get()) {
                configureNodeJs()
            }
            this@configureJsTarget.dependenciesFor("jsMain") {
                val useBom = p.multiPlatformHelper.useKotlinBom.orNull ?: false
                addMainDependencies(p.project, "js", skipBom = !useBom)
            }
            this@configureJsTarget.dependenciesFor("jsTest") {
                addTestDependencies(p.project, "js", skipAnnotations = true)
            }
            p.addMultiplatformTaskAliases("js")
        }
    }

    context (p: Project)
    protected fun KotlinJsTargetDsl.configureNodeJs() {
        nodejs {
            p.log("configure kotlin JS to target NodeJS")
            testTask(
                Action {
                    it.useMocha {
                        p.log("use Mocha as JS test framework")
                        timeout = p.project.multiPlatformHelper.mochaTimeout.orNull ?: timeout
                        p.log("set Mocha per-test-case timeout to $timeout")
                    }
                },
            )
        }
    }

    context (p: Project, jsTarget: KotlinJsTargetDsl)
    protected fun configureJsForBrowser() {
        jsTarget.browser {
            p.log("project configured to use Kotlin JS Browser target")
            webpackTask { webpack ->
                p.multiPlatformHelper.jsWebPackMode.orNull?.let {
                    webpack.mode = it
                    p.log("set webpack mode to $it")
                }
                webpack.mainOutputFileName.set(
                    p.multiPlatformHelper.jsWebPackOutputFileName.map {
                        p.log("set webpack main output file name to $it")
                        it
                    },
                )
            }
        }
    }

    context(p: Project)
    protected fun KotlinJsTargetDsl.configureJsModuleSystem() {
        when (p.multiPlatformHelper.jsModuleSystem.orNull) {
            JsModuleSystem.COMMON_JS -> {
                useCommonJs()
                p.log("configure kotlin JS to use CommonJS module system")
            }

            JsModuleSystem.ES_MODULES -> {
                useEsModules()
                p.log("configure kotlin JS to use ES module system")
            }

            else -> {
                p.log("configure kotlin JS to use UMD module system")
            }
        }
    }

    context(p: Project)
    protected fun KotlinJsBinaryContainer.configureAutomatically() {
        when (p.multiPlatformHelper.jsBinaryType.orNull) {
            JsBinaryType.LIBRARY -> {
                library()
                p.log("configure kotlin js to produce a library")
            }

            JsBinaryType.EXECUTABLE -> {
                executable()
                p.log("configure kotlin js to produce an executable")
            }

            else -> {}
        }
    }
}
