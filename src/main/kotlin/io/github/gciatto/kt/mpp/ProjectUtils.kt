@file:Suppress("TooManyFunctions")
package io.github.gciatto.kt.mpp

import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.json.PackageJson
import dev.petuska.npm.publish.extension.domain.json.Person
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.nio.charset.Charset

internal val Project.gradlePropertiesFile: File
    get() = projectDir.resolve("gradle.properties")

internal val Project.gradlePropertiesPath: String
    get() = gradlePropertiesFile.path

fun Project.log(message: String, logLevel: LogLevel = LogLevel.INFO) {
    logger.log(logLevel, "$name: $message")
}

fun Project.kotlinVersion(version: String) = kotlinVersion(provider { version })

fun Project.kotlinVersion(provider: Provider<String>) {
    val version = provider.getOrElse(KotlinVersion.CURRENT.toString())
    configurations.all { configuration ->
        configuration.resolutionStrategy.eachDependency { dependency ->
            if (dependency.requested.let { it.group == "org.jetbrains.kotlin" && it.name.startsWith("kotlin") }) {
                dependency.useVersion(version)
                dependency.because("All Kotlin modules should use the same version, and compiler uses $version")
            }
        }
    }
    log("enforce version for Kotlin dependencies: $version")
}

private fun String.getAsFile(charset: Charset = Charsets.UTF_8) =
    File(this).readText(charset)

fun String.getAsEitherFileOrValue(project: Project, charset: Charset = Charsets.UTF_8): String =
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
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = version.toString()
            log("set $path.jvmTarget=$jvmTarget")
        }
    }
}

val Project.npmCompliantVersion: String
    get() = version.toString().split("+")[0]

fun Project.nodeVersion(default: String, override: Any? = null) =
    nodeVersion(provider { default }, override)

fun Project.nodeVersion(default: Provider<String>, override: Any? = null) {
    plugins.withType<NodeJsRootPlugin> {
        configure<NodeJsRootExtension> {
            val requestedVersion = override?.toString()?.takeIf { it.isNotBlank() }
                ?: default.takeIf { it.isPresent }?.get()
                ?: nodeVersion
            nodeVersion = NodeVersions.toFullVersion(requestedVersion)
            log("set nodeVersion=$nodeVersion")
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
