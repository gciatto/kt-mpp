package io.github.gciatto.kt.mpp

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import java.util.Locale
import kotlin.jvm.optionals.asSequence

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

    private fun Project.getVersionFromCatalog(name: String, catalog: String? = null): VersionConstraint? {
        var catalogs = sequenceOf(project, rootProject)
            .map { it.extensions.findByType(VersionCatalogsExtension::class.java) }
            .filterNotNull()
            .flatMap { it.asSequence() }
            .toList()
        if (!catalog.isNullOrBlank()) {
            catalogs = catalogs.filter { it.name == catalog }
        }
        return catalogs.asSequence().flatMap { it.findVersion(name).asSequence() }.firstOrNull().also {
            if (it == null && catalogs.isEmpty()) {
                log(
                    message = "failed attempt to find version of `$name` in catalog" +
                        if (catalog == null) "s" else " $catalog",
                    logLevel = LogLevel.WARN,
                )
            }
        }
    }

    protected fun Project.configureKotlinVersionFromCatalogIfPossible() {
        val catalog = multiPlatformHelper.versionsFromCatalog.orNull
        val version = getVersionFromCatalog("kotlin", catalog)
        version?.requiredVersion?.let { kotlinVersion(it) }
    }

    protected fun Project.configureJvmVersionFromCatalogIfPossible() {
        val catalog = multiPlatformHelper.versionsFromCatalog.orNull
        val version = getVersionFromCatalog("jvm", catalog)
        version?.requiredVersion?.let { jvmVersion(it) }
    }

    protected fun Project.configureNodeVersionFromCatalogIfPossible() {
        val catalog = multiPlatformHelper.versionsFromCatalog.orNull
        val version = getVersionFromCatalog("node", catalog)
        nodeVersion(provider { version?.requiredVersion }, multiPlatformHelper.nodeVersion.orNull)
    }

    protected fun kotlinPlugin(name: String = targetName) =
        io.github.gciatto.kt.mpp.kotlinPlugin(name)

    context(Project)
    protected fun KotlinJvmOptions.configureJvmKotlinOptions(target: String) {
        multiPlatformHelper.ktCompilerArguments.configureEach {
            freeCompilerArgs += it
            log("add JVM-specific free compiler arg for $target: $it")
        }
    }

    context(Project)
    protected fun KotlinJsOptions.configureJsKotlinOptions(target: String) {
        main = "noCall"
        multiPlatformHelper.ktCompilerArguments.configureEach {
            freeCompilerArgs += it
            log("add JVM-specific free compiler arg for $target: $it")
        }
        multiPlatformHelper.ktCompilerArgumentsJs.configureEach {
            freeCompilerArgs += it
            log("add JS-specific free compiler arg for $target: $it")
        }
    }

    context(Project)
    protected fun KotlinCommonOptions.configureKotlinOptions(target: String) {
        allWarningsAsErrors = multiPlatformHelper.allWarningsAsErrors.get()
        if (allWarningsAsErrors) {
            log("consider all warnings as errors when compiling Kotlin sources in $target")
        }
        multiPlatformHelper.ktCompilerArguments.configureEach {
            freeCompilerArgs += it
            log("add free compiler arg for $target: $it")
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

    protected fun targetCompilationId(task: KotlinCompile<*>): String =
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
}
