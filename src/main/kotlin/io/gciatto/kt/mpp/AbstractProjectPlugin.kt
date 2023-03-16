package io.gciatto.kt.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionContainer
import kotlin.reflect.KClass

abstract class AbstractProjectPlugin : Plugin<Project> {
    protected fun Project.log(message: String, logLevel: LogLevel = LogLevel.LIFECYCLE) {
        logger.log(logLevel, "$name: $message")
    }

    protected abstract fun Project.applyThisPlugin()

    final override fun apply(target: Project) =
        target.applyThisPlugin()

    protected fun <T : Plugin<Project>> Project.apply(klass: KClass<T>): T =
        plugins.apply(klass.java)

    protected fun <T : Any> Project.configure(klass: KClass<T>, action: T.() -> Unit) {
        extensions.getByType(klass.java).run(action)
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

    protected fun <T : Any> ExtensionContainer.create(
        name: String,
        klass: KClass<T>,
        vararg constructorArguments: Any
    ): T = create(name, klass.java, *constructorArguments)

    protected fun <T : Any> ExtensionContainer.getByType(klass: KClass<T>): T = getByType(klass.java)
}
