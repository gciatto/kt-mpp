package io.github.gciatto.kt.mpp

import dev.petuska.npm.publish.extension.NpmPublishExtension
import io.github.gciatto.kt.mpp.helpers.MultiPlatformHelperExtensionImpl
import io.github.gciatto.kt.mpp.utils.forEachPlugin
import io.github.gciatto.kt.mpp.utils.kotlinPlugin
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.npmCompliantVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import java.util.Locale
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
abstract class AbstractProjectPlugin : Plugin<Project> {

    companion object {
        @JvmStatic
        protected val SUPPORTED_KOTLIN_TARGETS = setOf("jvm", "js", "multiplatform")
    }

    protected abstract fun Project.applyThisPlugin()

    private fun Project.addMultiPlatformHelperExtensionIfNecessary() {
        if (extensions.findByName("multiPlatformHelper") == null) {
            extensions.create("multiPlatformHelper", MultiPlatformHelperExtensionImpl::class.java, this)
        }
    }

    override fun apply(target: Project) {
        target.rootProject.addMultiPlatformHelperExtensionIfNecessary()
        target.addMultiPlatformHelperExtensionIfNecessary()
        target.applyThisPlugin()
    }

    context(Project)
    protected fun <T : Any> Provider<T>.getLogging(template: String): Provider<T> =
        map {
            log(template.format(it))
            it
        }

    context(Project)
    protected fun <T : Any> Provider<T>.asStringLogging(template: String): Provider<String> =
        map {
            log(template.format(it))
            it.toString()
        }

    protected fun <T : Plugin<Project>> Project.apply(klass: KClass<T>): T =
        plugins.apply(klass.java)

    protected fun <T : Any> Project.configure(klass: KClass<T>, action: T.() -> Unit) {
        extensions.getByType(klass.java).run(action)
    }

    protected fun Task.sibling(name: String) =
        path.split(":").let {
            it.subList(0, it.lastIndex) + name
        }.joinToString(":")

    protected val Project.isRootProject
        get() = this == rootProject

    protected fun <T : Any> ExtensionContainer.create(
        name: String,
        klass: KClass<T>,
        vararg constructorArguments: Any,
    ): T = create(name, klass.java, *constructorArguments)

    protected fun <T : Any> ExtensionContainer.getByType(klass: KClass<T>): T = getByType(klass.java)

    context(Project)
    protected fun NpmPublishExtension.syncNpmVersionWithProject() {
        version.set(
            provider {
                project.npmCompliantVersion.also {
                    log("set NPM publication version to $it")
                }
            },
        )
    }

    private fun Project.makeAssembleTaskDependOnJarTask(task: Jar) {
        tasks.matching { it.name == "assemble" }.all { assemble ->
            assemble.dependsOn(task)
            log("make ${assemble.path} task dependant on ${task.path}")
        }
    }

    context(Project)
    protected fun MavenPublication.addJarTask(task: Jar) {
        artifact(task)
        log("add task ${task.path} to publication $name, as javadoc artifact")
    }

    protected fun Project.createJarTask(name: String, classifier: String, group: String, action: Jar.() -> Unit): Jar =
        tasks.maybeCreate(name, Jar::class.java).also {
            it.group = group
            it.archiveClassifier.set(classifier)
            makeAssembleTaskDependOnJarTask(it)
            it.duplicatesStrategy = DuplicatesStrategy.WARN
            it.action()
        }

    @Suppress("MagicNumber")
    protected fun String?.asField() = when {
        this == null -> "null"
        else -> "'${replace("'", "\\'")}'"
    }

    @Suppress("MagicNumber")
    protected fun String?.asPassword() = when {
        this == null -> "null"
        else -> (1..kotlin.math.min(length, 8)).map { '*' }.joinToString("")
    }

    protected fun String.capital(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    protected fun Project.forAllKotlinPlugins(action: (Plugin<*>) -> Unit) {
        forEachKotlinPlugin(SUPPORTED_KOTLIN_TARGETS, action)
    }

    protected fun Project.forEachKotlinPlugin(names: Iterable<String>, action: (Plugin<*>) -> Unit) {
        forEachPlugin(names.map { kotlinPlugin(it) }, action)
    }

    protected fun Project.forEachKotlinPlugin(name: String, vararg names: String, action: (Plugin<*>) -> Unit) {
        forEachKotlinPlugin(listOf(name, *names), action)
    }
}
