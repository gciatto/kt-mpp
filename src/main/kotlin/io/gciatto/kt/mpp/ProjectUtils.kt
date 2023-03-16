@file:Suppress("TooManyFunctions")
package io.gciatto.kt.mpp

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

fun Project.jvmVersion(provider: Provider<String>) {
    val version = provider.map { JavaVersion.toVersion(it) }.getOrElse(JavaVersion.current())
    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            targetCompatibility = version
            logger.log(LogLevel.LIFECYCLE, "${rootProject.name} set java.targetCompatibility=$targetCompatibility")
            sourceCompatibility = version
            logger.log(LogLevel.LIFECYCLE, "${rootProject.name} set java.sourceCompatibility=$sourceCompatibility")
        }
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            logger.log(LogLevel.LIFECYCLE, "${rootProject.name} set $path.jvmTarget=$version")
            jvmTarget = version.toString()
        }
    }
}

val Project.npmCompliantVersion: String
    get() = version.toString().split("+")[0]

fun Project.nodeVersion(default: Provider<String>, override: Any? = null) {
    plugins.withType<NodeJsRootPlugin> {
        configure<NodeJsRootExtension> {
            val requestedVersion = override?.toString() ?: default.takeIf { it.isPresent }?.get() ?: nodeVersion
            nodeVersion = NodeVersions.toFullVersion(requestedVersion)
            logger.log(LogLevel.LIFECYCLE, "${rootProject.name} set nodeVersion=$nodeVersion")
        }
    }
}

fun Project.packageJson(handler: PackageJson.() -> Unit) {
    plugins.withType<NpmPublishPlugin> {
        configure<NpmPublishExtension> {
            packages {
                it.all {
                    packageJson(handler)
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

val Provider<MinimalExternalModuleDependency>.version
    get() = this.get().versionConstraint.requiredVersion
