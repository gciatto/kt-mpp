package io.gciatto.kt.mpp

import org.gradle.api.Project
import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import io.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.gradle.api.logging.LogLevel


class PublishOnNpm : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val npmPublish = apply<NpmPublishPlugin>()
        log("apply ${npmPublish::class.java.name} plugin")
        configure<NpmPublishExtension> {
            getOptionalProperty("npmOrganization")?.let {
                organization.set(it)
            } ?: log("property npmOrganization unset", LogLevel.WARN)
            rootProject.file("README.md").takeIf { it.exists() }?.let {
                readme.set(it)
            }
            // bundleKotlinDependencies.set(true)
            getOptionalProperty("npmDryRun").let {
                dry.set(it?.toBoolean() ?: false)
            }
            registries { registries ->
                registries.npmjs { npm ->
                    getOptionalProperty("npmToken")?.let {
                        npm.authToken.set(it)
                    } ?: log("property npmToken unset", LogLevel.WARN)
                }
            }
            packages { packages ->
                packages.all { pkg ->
                    pkg.packageName.set("${rootProject.name}-${project.name}")
                    log("set JS package name to ${pkg.packageName}")
                    packageJson {
                        getOptionalProperty("projectHomepage")?.let {
                            homepage.set(it)
                            log("set package.json homepage to $it")
                        } ?: log("property projectHomepage unset", LogLevel.WARN)
                        getOptionalProperty("projectDescription")?.let {
                            description.set(it)
                            log("set package.json description to '$it'")
                        } ?: log("property projectDescription unset", LogLevel.WARN)
                        val developers = project.getAllDevs()
                        if (developers.isNotEmpty()) {
                            val mainDeveloper = developers.first()
                            author.set(person(mainDeveloper))
                            log("set package.json author to $mainDeveloper")
                        }
                        contributors.set(
                            developers.asSequence()
                                .drop(1)
                                .map { person(it) }
                                .toCollection(mutableListOf())
                                .also { log("add package.json contributors: ${it.joinToString()}") }
                        )
                        getOptionalProperty("projectLicense")?.let {
                            license.set(it)
                            log("set package.json license to $it")
                        } ?: log("property projectLicense unset", LogLevel.WARN)
                        private.set(false)
                        bugs { bugs ->
                            getOptionalProperty("issuesUrl")?.let {
                                bugs.url.set(it)
                                log("set package.json bug URL to $it")
                            } ?: log("property issuesUrl unset", LogLevel.WARN)
                            getOptionalProperty("issuesEmail")?.let {
                                bugs.email.set(it)
                                log("set package.json bug email to $it")
                            } ?: log("property issuesEmail unset", LogLevel.WARN)
                        }
                        repository { repos ->
                            repos.type.set("git")
                            getOptionalProperty("scmUrl")?.let {
                                repos.url.set(it)
                                log("set package.json repo URL to $it")
                            } ?: log("property scmUrl unset", LogLevel.WARN)
                        }
                    }
                }
            }
        }
    }
}
