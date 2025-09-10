@file:Suppress("TooManyFunctions")

package io.github.gciatto.kt.mpp.utils

import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.json.PackageJson
import dev.petuska.npm.publish.extension.domain.json.Person
import io.github.gciatto.kt.mpp.helpers.MultiPlatformHelperExtension
import io.github.gciatto.kt.mpp.publishing.Developer
import io.github.gciatto.kt.mpp.utils.JvmVersions.toJvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File
import java.nio.charset.Charset
import kotlin.jvm.optionals.asSequence
import kotlin.reflect.KClass

internal fun kotlinPlugin(name: String) = "org.jetbrains.kotlin.$name"

internal val Project.gradlePropertiesFile: File
    get() = projectDir.resolve("gradle.properties")

internal val Project.gradlePropertiesPath: String
    get() = gradlePropertiesFile.path

internal val Project.multiPlatformHelper: MultiPlatformHelperExtension
    get() = extensions.getByType(MultiPlatformHelperExtension::class.java)

fun Project.log(
    message: String,
    logLevel: LogLevel = LogLevel.INFO,
) {
    logger.log(logLevel, "$name: $message")
}

fun Project.log(
    message: String?,
    vararg otherMessages: String?,
    logLevel: LogLevel = LogLevel.INFO,
    separator: String = "",
) = log(listOfNotNull(message, *otherMessages).joinToString(separator), logLevel)

fun Project.kotlinVersion(version: String) = kotlinVersion(provider { version })

fun Project.kotlinVersion(provider: Provider<String>) {
    val version = provider.getOrElse(KotlinVersion.CURRENT.toString())
    configurations.matching { "detekt" !in it.name }.all { configuration ->
        configuration.resolutionStrategy.eachDependency { dependency ->
            if (dependency.requested.let { it.group == "org.jetbrains.kotlin" && it.name.startsWith("kotlin") }) {
                dependency.useVersion(version)
                dependency.because(
                    "All Kotlin-related dependencies should use the same version," +
                        "and user selected version $version",
                )
                val artifact = "${dependency.requested.group}:${dependency.requested.name}"
                log("force version $version for dependency $artifact in configuration ${configuration.name}")
            }
        }
    }
}

private fun String.getAsFile(charset: Charset = Charsets.UTF_8) = File(this).readText(charset)

fun String.getAsEitherFileOrValue(
    project: Project,
    charset: Charset = Charsets.UTF_8,
): String =
    if (startsWith("file:")) {
        replace("\$rootProject", project.rootProject.projectDir.absolutePath)
            .replace("\$project", project.projectDir.absolutePath)
            .replace("file:", "")
            .getAsFile(charset)
    } else {
        this
    }

fun Project.jvmVersion(version: String) = jvmVersion(provider { version })

fun Project.jvmVersion(provider: Provider<String>) {
    val version = provider.map { JavaVersion.toVersion(it) }.getOrElse(JavaVersion.current())
    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            targetCompatibility = version
            log("set java.targetCompatibility=$targetCompatibility")
            sourceCompatibility = version
            log("set java.sourceCompatibility=$sourceCompatibility")
        }
    }
    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(version.toJvmTarget())
            log("set $path.jvmTarget=$version")
        }
    }
    tasks.withType<JavaCompile> {
        sourceCompatibility = version.toString()
        log("set $path.sourceCompatibility=$sourceCompatibility")
        targetCompatibility = version.toString()
        log("set $path.targetCompatibility=$targetCompatibility")
    }
}

val Project.npmCompliantVersion: String
    get() = version.toString().split("+")[0]

fun Project.nodeVersion(
    default: String,
    override: Any? = null,
) = nodeVersion(provider { default }, override)

@Suppress("DEPRECATION_ERROR")
fun Project.nodeVersion(
    default: Provider<String>,
    override: Any? = null,
) {
    plugins.withType<NodeJsRootPlugin> {
        configure<NodeJsRootExtension> {
            val requestedVersion =
                override?.toString()?.takeIf { it.isNotBlank() }
                    ?: default.takeIf { it.isPresent }?.get()
                    ?: version
            version = NodeVersions.latest(requestedVersion)
            log("set nodeVersion=$version")
        }
    }
}

fun Project.packageJson(handler: PackageJson.() -> Unit) {
    plugins.withType<NpmPublishPlugin> {
        configure<NpmPublishExtension> {
            packages { npmPackages ->
                npmPackages.all { npmPackage ->
                    npmPackage.packageJson(handler)
                }
            }
        }
    }
}

fun PackageJson.person(developer: Developer): Person =
    this.Person {
        it.name.set(developer.name)
        it.email.set(developer.email)
        it.url.set(developer.url)
    }

val Provider<MinimalExternalModuleDependency>.version: String
    get() = this.get().versionConstraint.requiredVersion

@Suppress("UNCHECKED_CAST")
fun Project.withPlugin(
    plugin: Any,
    action: (Plugin<*>) -> Unit,
) {
    with(project.plugins) {
        when (plugin) {
            is String -> withId(plugin, action)
            is Class<*> -> withType(plugin as Class<Plugin<Project>>, action)
            is KClass<*> -> withType(plugin as KClass<Plugin<Project>>, action)
            is Provider<*> -> forEachPlugin(listOf(plugin.get()), action)
            else -> error("Invalid plugin: $plugin")
        }
    }
}

fun Project.forEachPlugin(
    plugins: Iterable<Any>,
    action: (Plugin<*>) -> Unit,
) {
    for (plugin in plugins) {
        withPlugin(plugin, action)
    }
}

fun Project.forEachPlugin(
    name: Any,
    vararg names: Any,
    action: (Plugin<*>) -> Unit,
) {
    forEachPlugin(listOf(name, *names), action)
}

fun Project.ifAllPluginsAltogether(
    plugins: List<Any>,
    action: () -> Unit,
) {
    var outerAction: (Plugin<*>) -> Unit = { action() }
    for (plugin in plugins.subList(1, plugins.size).asReversed()) {
        outerAction = { withPlugin(plugin, outerAction) }
    }
    withPlugin(plugins.first(), outerAction)
}

fun Project.ifAllPluginsAltogether(
    name: Any,
    vararg names: Any,
    action: () -> Unit,
) {
    ifAllPluginsAltogether(listOf(name, *names), action)
}

internal val Project.jsPackageName: String
    get() =
        if (project == rootProject) {
            rootProject.name
        } else {
            "${rootProject.name}-${project.name}"
        }

internal fun String.toURL(): java.net.URL =
    java.net.URI
        .create(this)
        .toURL()

internal fun Project.getVersionFromCatalog(
    name: String,
    catalog: String? = null,
): VersionConstraint? {
    var catalogs =
        sequenceOf(project, rootProject)
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
                message =
                    "failed attempt to find version of `$name` in catalog" +
                        if (catalog == null) "s" else " $catalog",
                logLevel = LogLevel.WARN,
            )
        }
    }
}
