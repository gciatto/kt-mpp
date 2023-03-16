package io.gciatto.kt.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionContainer

abstract class AbstractProjectPlugin : Plugin<Project> {
    protected fun Project.log(message: String, logLevel: LogLevel = LogLevel.LIFECYCLE) {
        logger.log(logLevel, "$name: $message")
    }

    protected abstract fun Project.applyThisPlugin()

    final override fun apply(target: Project) =
        target.applyThisPlugin()

    protected inline fun <reified T : Plugin<Project>> Project.apply(): T =
        plugins.apply(T::class.java)

    protected inline fun <reified T> Project.configure(action: T.() -> Unit) {
        extensions.getByType(T::class.java).run(action)
    }

    protected fun Project.getProperty(name: String): String =
        property(name).toString()

    protected fun Project.getOptionalProperty(name: String): String? =
        findProperty(name)?.toString()

    protected fun Project.getBooleanProperty(name: String, default: Boolean = false): Boolean =
        findProperty(name)?.toString()?.toBooleanStrictOrNull() ?: default

    protected fun Task.sibling(name: String) =
        path.split(":").let {
            it.subList(0, it.lastIndex) + name
        }.joinToString(":")

    protected val Project.isRootProject
        get() = this == rootProject

    protected inline fun <reified T> ExtensionContainer.create(name: String, vararg constructorArguments: Any): T =
        create(name, T::class.java, *constructorArguments)

    protected inline fun <reified T> ExtensionContainer.getByType(): T = getByType(T::class.java)
}
