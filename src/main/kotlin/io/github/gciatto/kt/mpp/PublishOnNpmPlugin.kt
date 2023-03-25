package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.NpmRegistry
import io.github.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
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

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(npmOrganization)
        addProperty(npmDryRun)
        addProperty(npmRepo)
        addProperty(npmToken)
        addProperty(projectHomepage)
        addProperty(projectDescription)
        addProperty(projectLicense)
        addProperty(issuesUrl)
        addProperty(issuesEmail)
        addProperty(scmUrl)
        addProperty(developerIdName)
        addProperty(developerIdUrl)
        addProperty(developerIdEmail)
        addProperty(developerIdOrg)
        addProperty(orgName)
        addProperty(orgUrl)
    }

    @Suppress("CyclomaticComplexMethod")
    override fun Project.applyThisPlugin() {
        val npmPublish = apply(NpmPublishPlugin::class)
        log("apply ${npmPublish::class.java.name} plugin")
        plugins.withId("org.danilopianini.publish-on-central") {
            configure(PublishOnCentralExtension::class) {
                configureNpmPublishing(this)
            }
        }
    }

    private fun Project.configureNpmPublishing(centralExtension: PublishOnCentralExtension) =
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
                    registries.create("custom") {
                        it.uri.set(uri(npmRepo))
                        configureRegistry(it)
                    }
                }
            }
            packages { packages ->
                packages.all { pkg ->
                    pkg.packageName.set("${rootProject.name}-${project.name}")
                    log("set JS package name to ${pkg.packageName}")
                    packageJson {
                        homepage.set(centralExtension.projectUrl)
                        log("set package.json homepage to match the POM project URL")
                        description.set(centralExtension.projectDescription)
                        log("set package.json description to match the POM project description")
                        license.set(centralExtension.licenseName)
                        log("set package.json license to match the POM license name")
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
                            repos.url.set(centralExtension.scmConnection)
                            log("set package.json repo URL to match the POM SCM connection")
                        }
                    }
                }
            }
        }
}
