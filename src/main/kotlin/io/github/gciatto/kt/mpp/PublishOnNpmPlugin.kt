package io.github.gciatto.kt.mpp

import dev.petuska.npm.publish.NpmPublishPlugin
import dev.petuska.npm.publish.extension.NpmPublishExtension
import dev.petuska.npm.publish.extension.domain.NpmPackage
import dev.petuska.npm.publish.extension.domain.NpmRegistry
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class PublishOnNpmPlugin : AbstractProjectPlugin() {
    context (Project)
    private fun configureRegistry(registry: NpmRegistry) {
        registry.uri.map { log("configure publication on registry: $it") }
        registry.authToken.set(multiPlatformHelper.npmToken.getLogging("use NPM token for publication: %s"))
    }

    @Suppress("CyclomaticComplexMethod")
    override fun Project.applyThisPlugin() {
        val npmPublish = apply(NpmPublishPlugin::class)
        log("apply ${npmPublish::class.java.name} plugin")
        if (multiPlatformHelper.jsBinaryType.let { it.isPresent && it.get() != JsBinaryType.LIBRARY }) {
            log(
                "publication on NPM via ${npmPublish::class.java.name} plugin will fail because " +
                    "jsBinaryType is not set to LIBRARY",
                LogLevel.WARN,
            )
        }
        configureNpmPublishing()
    }

    context (Project)
    private fun NpmPublishExtension.configureNpmRepositories() {
        registries { registries ->
            val npmRepo = multiPlatformHelper.npmRepo.orNull
            if (npmRepo === null || "npmjs.org" in npmRepo.host) {
                registries.npmjs { configureRegistry(it) }
            } else {
                val name = npmRepo.host.split('.').joinToString("") { it.capital() }
                registries.create(name) {
                    it.uri.set(uri(npmRepo))
                    configureRegistry(it)
                }
            }
        }
    }

    context (Project, NpmPublishExtension)
    private fun NpmPackage.configureNpmPackages() {
        val mpp = multiPlatformHelper
        packageName.set(mpp.jsPackageName.getLogging("set NPM package name: %s"))
        packageJson { pkg ->
            pkg.homepage.set(mpp.projectHomepage.asStringLogging("set package.json homepage: %s"))
            pkg.description.set(mpp.projectDescription.getLogging("set package.json description: %s"))
            pkg.license.set(mpp.projectLicense.getLogging("set package.json license: %s"))
            val developers = mpp.developers.toList()
            if (developers.isNotEmpty()) {
                val mainDeveloper = developers[0]
                pkg.author.set(pkg.person(mainDeveloper))
                log("set package.json author to $mainDeveloper")
            }
            if (developers.size > 1) {
                val contributors = developers.subList(1, developers.size)
                pkg.contributors.set(contributors.map { pkg.person(it) })
                val contributorsList = contributors.joinToString(prefix = "[", postfix = "]")
                log("add package.json contributors: $contributorsList")
            }
            pkg.private.set(false)
            log("about to set package.json bugs")
            pkg.bugs { bugs ->
                bugs.url.set(mpp.issuesUrl.asStringLogging("set package.json bug URL to %s"))
                bugs.email.set(mpp.issuesEmail.getLogging("set package.json bug email to %s"))
            }
            log("about to set package.json repositories")
            pkg.repository { repos ->
                repos.type.set("git")
                repos.url.set(mpp.scmUrl.asStringLogging("set package.json repo URL to %s"))
            }
            log("overridden package.json: ${pkg.finalise()}")
        }
    }

    private fun Project.configureNpmPublishing() =
        configure(NpmPublishExtension::class) {
            val mpp = multiPlatformHelper
            organization.set(mpp.npmOrganization.getLogging("set NPM organization: %s"))
            listOf(project, rootProject).map { it.file("README.md") }.firstOrNull { it.exists() }?.let {
                readme.set(it)
                log("include file ${it.path} into NPM publication")
            }
            syncNpmVersionWithProject()
            // bundleKotlinDependencies.set(true)
            dry.set(mpp.npmDryRun.getLogging("set NPM dry run: %s"))
            configureNpmRepositories()
            packages { packages ->
                packages.all { pkg ->
                    pkg.configureNpmPackages()
                }
            }
            version.set(project.provider { project.npmCompliantVersion })
        }
}
