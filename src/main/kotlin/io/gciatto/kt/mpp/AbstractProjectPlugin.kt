package io.gciatto.kt.mpp

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel

abstract class AbstractProjectPlugin : Plugin<Project> {
    protected fun Project.log(message: String, logLevel: LogLevel = LogLevel.LIFECYCLE) {
        logger.log(logLevel, "${name}: $message")
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
}
