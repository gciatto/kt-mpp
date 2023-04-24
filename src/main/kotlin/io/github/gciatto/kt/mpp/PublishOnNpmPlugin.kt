package io.github.gciatto.kt.mpp

import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.NpmPackage
import dev.petuska.npm.publish.extension.domain.NpmRegistry
import io.github.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
import org.gradle.api.Project
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

    context (Project)
    private fun NpmPublishExtension.configureNpmRepositories() {
        registries { registries ->
            val npmRepo = getOptionalProperty("npmRepo")
            if (npmRepo.isNullOrBlank() || npmRepo == getPropertyDescriptor("npmRepo").defaultValue) {
                registries.npmjs { configureRegistry(it) }
            } else {
                registries.create("custom") {
                    it.uri.set(uri(npmRepo))
                    configureRegistry(it)
                }
            }
        }
    }

    context (Project, NpmPublishExtension)
    private fun NpmPackage.configureNpmPackages(centralExtension: PublishOnCentralExtension) {
        project.afterEvaluate { _ ->
            packageName.set(
                "${rootProject.name}-${project.name}".also {
                    log("set JS package name to $it")
                },
            )
        }
        this.packageJson { pkg ->
            pkg.homepage.set(
                centralExtension.projectUrl.map {
                    it.also { log("set package.json homepage to $it") }
                },
            )
            pkg.description.set(
                centralExtension.projectDescription.map {
                    it.also { log("set package.json description to $it") }
                },
            )
            pkg.license.set(
                centralExtension.licenseName.map {
                    it.also { log("set package.json license to $it") }
                },
            )
            val developers = project.getAllDevs()
            if (developers.isNotEmpty()) {
                val mainDeveloper = developers.first()
                pkg.author.set(pkg.person(mainDeveloper))
                log("set package.json author to $mainDeveloper")
            }
            pkg.contributors.set(
                developers.asSequence()
                    .drop(1)
                    .map { pkg.person(it) }
                    .toCollection(mutableListOf())
                    .also {
                        val contributorsList = it.joinToString(prefix = "[", postfix = "]")
                        log("add package.json contributors: $contributorsList")
                    },
            )
            pkg.private.set(false)
            pkg.bugs { bugs ->
                getOptionalProperty("issuesUrl")?.let {
                    bugs.url.set(it)
                    log("set package.json bug URL to $it")
                }
                getOptionalProperty("issuesEmail")?.let {
                    bugs.email.set(it)
                    log("set package.json bug email to $it")
                }
            }
            pkg.repository { repos ->
                repos.type.set("git")
                repos.url.set(
                    centralExtension.scmConnection.map {
                        it.also { log("set package.json repo URL to $it") }
                    },
                )
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
                if (it) {
                    log(
                        "dry-run mode for NPM publishing plugin: no package will actually be release on NPM!",
                        LogLevel.WARN,
                    )
                }
            }
            configureNpmRepositories()
            packages { packages ->
                packages.all { pkg ->
                    pkg.configureNpmPackages(centralExtension)
                }
            }
        }
}
