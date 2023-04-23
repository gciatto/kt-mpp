package io.github.gciatto.kt.mpp

import io.github.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.danilopianini.gradle.mavencentral.DocStyle
import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
import org.danilopianini.gradle.mavencentral.Repository
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.apply
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

class PublishOnMavenPlugin : AbstractProjectPlugin() {

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(docStyle)
        addProperty(repoOwner)
        addProperty(mavenCentralPassword)
        addProperty(mavenCentralUsername)
        addProperty(otherMavenRepo)
        addProperty(otherMavenUsername)
        addProperty(otherMavenPassword)
        addProperty(signingKey)
        addProperty(signingPassword)
        addProperty(projectLongName)
        addProperty(projectDescription)
        addProperty(projectHomepage)
        addProperty(projectLicense)
        addProperty(projectLicenseUrl)
        addProperty(scmConnection)
        addProperty(scmUrl)
        addProperty(developerIdName)
        addProperty(developerIdUrl)
        addProperty(developerIdEmail)
        addProperty(developerIdOrg)
        addProperty(orgName)
        addProperty(orgUrl)
    }

    context(Project)
    private fun Repository.configure(username: String?, pwd: String?) {
        if (username != null && pwd != null) {
            user.set(username)
            password.set(pwd)
            /* ktlint-disable */
            log(
                "configure Maven repository $name " +
                    "(URL: $url, username: ${user.get().asField()}, " +
                    "password: ${password.get().asPassword()})"
            )
            /* ktlint-enable */
        }
    }

    private fun Project.configureMavenRepositories() = configure(PublishOnCentralExtension::class) {
        configureMavenCentral.set(true)
        mavenCentral.run {
            val mavenCentralUsername: String? = getOptionalProperty("mavenCentralUsername")
            val mavenCentralPassword: String? = getOptionalProperty("mavenCentralPassword")
            configure(mavenCentralUsername, mavenCentralPassword)
        }
        getOptionalProperty("otherMavenRepo")?.takeIf { it.isNotBlank() && "oss.sonatype.org" !in it }?.let {
            val mavenUsername: String? = getOptionalProperty("otherMavenUsername")
            val mavenPassword: String? = getOptionalProperty("otherMavenPassword")
            repository(it) {
                user.set(mavenUsername)
                password.set(mavenPassword)
                /* ktlint-disable */
                log(
                    "configure Maven repository $name " +
                        "(URL: $it, username: ${user.get().asField()}, " +
                        "password: ${password.get().asPassword()})"
                )
                /* ktlint-enable */
            }
        }
    }

    private fun Project.configureSigning() = configure(SigningExtension::class) {
        val signingKey: String? = getOptionalProperty("signingKey")
        val signingPassword: String? = getOptionalProperty("signingPassword")
        if (arrayOf(signingKey, signingPassword).none { it.isNullOrBlank() }) {
            val actualKey = signingKey!!.getAsEitherFileOrValue(project)
            val actualPassphrase = signingPassword!!.getAsEitherFileOrValue(project)
            log(
                "configure signatory for publication for project $name: " +
                    "key=${actualKey.asPassword()}, passphrase=${actualPassphrase.asPassword()}"
            )
            useInMemoryPgpKeys(actualKey, actualPassphrase)
        } else {
            /* ktlint-disable */
            log(
                "one property in {signingKey, signingPassword} is unset or blank, " +
                    "hence Maven publications won't be signed"
            )
            /* ktlint-enable */
        }
        val signAll = tasks.create("signAllPublications") { it.group = "signing" }
        tasks.withType(Sign::class.java) {
            it.group = "signing"
            signAll.dependsOn(it)
            log("make ${signAll.path} tasks dependant on ${it.path}")
        }
    }

    private fun Project.configurePublications() = configure(PublishOnCentralExtension::class) {
        getOptionalProperty("projectLongName")?.let {
            projectLongName.set(it)
            log("set POM name: $it")
        }
        (description ?: getOptionalProperty("projectDescription"))?.let {
            projectDescription.set(it)
            log("set POM description: $it")
        }
        getOptionalProperty("repoOwner")?.takeIf { it.isNotBlank() }?.let {
            repoOwner.set(it)
            log("set repoOwner: $it")
        }
        getOptionalProperty("projectHomepage")?.takeIf { it.isNotBlank() }?.let {
            projectUrl.set(it)
            log("set POM URL: $it")
        }
        getOptionalProperty("projectLicense")?.takeIf { it.isNotBlank() }?.let {
            licenseName.set(it)
            log("set POM license name: $it")
        }
        getOptionalProperty("projectLicenseUrl")?.takeIf { it.isNotBlank() }?.let {
            licenseUrl.set(it)
            log("set POM license URL: $it")
        }
        getOptionalProperty("scmConnection")?.takeIf { it.isNotBlank() }?.let {
            scmConnection.set(it)
            log("add POM SCM connection: $it")
        }
        addMissingInformationToPublications()
    }

    private fun Project.addMissingInformationToPublications() = configure(PublishingExtension::class) {
        publications.withType(MavenPublication::class.java) { pub ->
            pub.pom { pom ->
                pom.developers { devs ->
                    for (dev in project.getAllDevs()) {
                        dev.applyTo(devs)
                        log("add POM developer publication ${pub.name}: $dev")
                    }
                }
                pom.scm { scm ->
                    getOptionalProperty("scmUrl")?.takeIf { it.isNotBlank() }?.let {
                        scm.url.set(it)
                        log("add POM SCM URL in publication ${pub.name}: $it")
                    }
                }
                pom.issueManagement { issues ->
                    getOptionalProperty("issuesUrl")?.takeIf { it.isNotBlank() }?.let {
                        issues.url.set(it)
                        log("set POM issue URL to $it")
                    }
                }
            }
        }
    }

    private val defaultMppPublications = setOf("kotlinMultiplatform", "js", "jvm")

    private val publishTasks by lazy {
        defaultMppPublications.map { "publish${it.capital()}Publication" }
    }

    private fun Project.configurePublishOnCentralExtension() = configure(PublishOnCentralExtension::class) {
        autoConfigureAllPublications.set(true)
    }

    private fun Project.fixSignPublishTaskDependencies() {
        tasks.withType(Sign::class.java) { before ->
            tasks.withType(AbstractPublishToMaven::class.java) { after ->
                after.mustRunAfter(before)
                log("make task ${after.path} run after ${before.path}")
            }
        }
    }

    private fun Project.fixMavenPublicationsJavadocArtifact() {
        plugins.withType(PublishOnMavenPlugin::class.java) { _ ->
            configure(PublishOnCentralExtension::class) {
                val docStyleString = getOptionalProperty("docStyle")
                val docStyleValue = DocStyle.values()
                    .filter { it.name.equals(docStyleString, ignoreCase = true) }
                    .firstOrNull()
                    ?: error("Invalid value for property dokkaArtifactInMavenPublication: $docStyleString")
                docStyle.set(docStyleValue)
                log("use ${docStyleValue.name} style for javadoc JAR when publishing")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun Project.applyThisPlugin() {
        fun configurePlugin(plugin: Plugin<*>) {
            apply(plugin = "org.danilopianini.publish-on-central")
            log("apply org.danilopianini.publish-on-central plugin")
            configurePublishOnCentralExtension()
            configureMavenRepositories()
            configurePublications()
            addMissingInformationToPublications()
            configureSigning()
            fixSignPublishTaskDependencies()
            fixMavenPublicationsJavadocArtifact()
        }
        forAllKotlinPlugins { configurePlugin(it) }
    }
}
