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

    private fun Project.helpMessage(name: String, mandatory: Boolean): String {
        val result = StringBuilder()
        if (mandatory) {
            result.append("Mandatory property $name is missing. What you can do:\n")
        } else {
            result.append("Optional property $name is missing. This is not an issue, but here's what you can do:\n")
        }
        result.append("1. invoke Gradle with -P$name=<value> option;\n")
        result.append("2. invoke Gradle after setting ORG_GRADLE_PROJECT_$name=<value> environment variable;\n")
        val gradlePropertiesFile = projectDir.resolve("gradle.properties").absolutePath
        result.append("3. add a line containing $name=<value> in the file $gradlePropertiesFile .\n")
        result.append("We recommend to do step 3. in any case, using a (possibly blank) default value, ")
        result.append("to avoid this message to be logged in the future.")
        return result.toString()
    }

    protected fun Project.getProperty(name: String): String {
        if (name !in properties) {
            log(helpMessage(name, mandatory = true), LogLevel.ERROR)
        }
        return property(name).toString()
    }

    protected fun Project.getOptionalProperty(name: String): String? {
        val result = findProperty(name)?.toString()
        if (result == null) {
            log(helpMessage(name, mandatory = false), LogLevel.WARN)
        }
        return result
    }

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
