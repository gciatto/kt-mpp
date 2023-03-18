package io.gciatto.kt.mpp

import org.gradle.api.Project
import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.NpmRegistry
import io.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.gradle.api.logging.LogLevel

class PublishOnNpmPlugin : AbstractProjectPlugin() {
    context (Project)
    private fun configureRegistry(registry: NpmRegistry) {
        registry.uri.map { log("configure publication on registry: $it") }
        getOptionalProperty("npmToken")?.let {
            registry.authToken.set(it)
            log("use NPM token for publication: ${it.asPassword()}")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun Project.applyThisPlugin() {
        val npmPublish = apply(NpmPublishPlugin::class)
        log("apply ${npmPublish::class.java.name} plugin")
        configure(NpmPublishExtension::class) {
            getOptionalProperty("npmOrganization")?.let {
                if (it.isNotBlank()) {
                    organization.set(it)
                    log("set NPM organization: $it")
                }
            }
            listOf(project, rootProject).map { it.file("README.md") }.firstOrNull { it.exists() }?.let {
                readme.set(it)
                log("include file ${it.path} into NPM publication")
            }
            syncNpmVersionWithProject()
            // bundleKotlinDependencies.set(true)
            getBooleanProperty("npmDryRun").let {
                dry.set(it)
                if (it) {
                    log(
                        "dry-run mode for NPM publishing plugin: no package will actually be release on NPM!",
                        LogLevel.WARN
                    )
                }
            }
            registries { registries ->
                val npmRepo = getOptionalProperty("npmRepo")
                if (npmRepo.isNullOrBlank()) {
                    registries.npmjs { configureRegistry(it) }
                } else {
                    registries.create("customNpmRegistry") { it.uri.set(uri(npmRepo)) }
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
                        }
                        getOptionalProperty("projectDescription")?.let {
                            description.set(it)
                            log("set package.json description to '$it'")
                        }
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
                        }
                        private.set(false)
                        bugs { bugs ->
                            getOptionalProperty("issuesUrl")?.let {
                                bugs.url.set(it)
                                log("set package.json bug URL to $it")
                            }
                            getOptionalProperty("issuesEmail")?.let {
                                bugs.email.set(it)
                                log("set package.json bug email to $it")
                            }
                        }
                        repository { repos ->
                            repos.type.set("git")
                            getOptionalProperty("scmUrl")?.let {
                                repos.url.set(it)
                                log("set package.json repo URL to $it")
                            }
                        }
                    }
                }
            }
        }
    }
}
