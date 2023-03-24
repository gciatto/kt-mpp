package io.github.gciatto.kt.mpp

import io.github.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

class PublishOnMavenPlugin : AbstractProjectPlugin() {

    @Suppress("MemberVisibilityCanBePrivate")
    lateinit var publishableClassifiers: DomainObjectSet<String>

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(mavenRepo)
        addProperty(mavenUsername)
        addProperty(mavenPassword)
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

    private fun Project.configureMavenRepository() {
        configure(PublishingExtension::class) {
            repositories { repos ->
                repos.maven { maven ->
                    getOptionalProperty("mavenRepo")?.let {
                        maven.url = uri(it)
                    }
                    val mavenUsername: String? = getOptionalProperty("mavenUsername")
                    val mavenPassword: String? = getOptionalProperty("mavenPassword")
                    if (mavenUsername != null && mavenPassword != null) {
                        maven.credentials {
                            it.username = mavenUsername
                            it.password = mavenPassword
                        }
                        /* ktlint-disable */
                        log(
                            "configure Maven repository ${maven.name} " +
                                "(URL: ${maven.url}, username: ${maven.credentials.username.asField()}, " +
                                "password: ${maven.credentials.password.asPassword()})"
                        )
                        /* ktlint-enable */
                    } else {
                        log("configure Maven repository ${maven.name} with no credentials", LogLevel.WARN)
                    }
                }
            }
        }
    }

    private fun Project.configureSigning() {
        configure(PublishingExtension::class) {
            configure(SigningExtension::class) {
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
                    publications.withType(MavenPublication::class.java) {
                        sign(it)
                        log("configure signing for publication: $it")
                    }
                } else {
                    /* ktlint-disable */
                    log(
                        "one property in {signingKey, signingPassword} is unset or blank, " +
                            "hence Maven publications won't be signed"
                    )
                    /* ktlint-enable */
                }
                val signAll = project.tasks.create("signAllPublications")
                project.tasks.withType(Sign::class.java) {
                    signAll.dependsOn(it)
                    log("make ${signAll.path} tasks dependant on ${it.path}")
                }
            }
        }
    }

    private fun Project.addMissingPublications() {
        configure(PublishingExtension::class) {
            configure(SigningExtension::class) {
                plugins.withId("org.jetbrains.kotlin.jvm") {
                    publications.maybeCreate("jvm", MavenPublication::class.java).run {
                        from(components.getAt("java"))
                        log("add jvm publication from java component")
                    }
                }
                plugins.withId("org.jetbrains.kotlin.js") {
                    publications.maybeCreate("js", MavenPublication::class.java).run {
                        from(components.getAt("kotlin"))
                        log("add js publication from kotlin component")
                    }
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun Project.lazilyConfigurePOM() {
        configure(PublishingExtension::class) {
            afterEvaluate { project ->
                publications.withType(MavenPublication::class.java) { pub ->
                    pub.copyMavenGroupAndVersionFromProject()
                    pub.pom { pom ->
                        getOptionalProperty("projectLongName")?.let {
                            pom.name.set(it)
                            log("set POM name in publication ${pub.name}: $it")
                        }
                        getOptionalProperty("projectDescription")?.let {
                            pom.description.set(it)
                            log("set POM description in publication ${pub.name}: $it")
                        }
                        getOptionalProperty("projectHomepage")?.let {
                            pom.url.set(it)
                            log("set POM URL in publication ${pub.name}: $it")
                        }
                        pom.licenses { licenses ->
                            licenses.license { license ->
                                getOptionalProperty("projectLicense")?.let {
                                    license.name.set(it)
                                    log("add POM license in publication ${pub.name}: $it")
                                }
                                getOptionalProperty("projectLicenseUrl")?.let {
                                    license.url.set(it)
                                    log("add POM license URL in publication ${pub.name}: $it")
                                }
                            }
                        }
                        pom.developers { devs ->
                            for (dev in project.getAllDevs()) {
                                dev.applyTo(devs)
                                log("add POM developer publication ${pub.name}: $dev")
                            }
                        }
                        pom.scm { scm ->
                            getOptionalProperty("scmConnection")?.let {
                                scm.connection.set(it)
                                log("add POM SCM connection in publication ${pub.name}: $it")
                            }
                            getOptionalProperty("scmUrl")?.let {
                                scm.url.set(it)
                                log("add POM SCM URL in publication ${pub.name}: $it")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun Project.applyThisPlugin() {
        publishableClassifiers = project.objects.domainObjectSet(String::class.java)
        publishableClassifiers.add("javadoc")
        apply(plugin = "maven-publish")
        log("apply maven-publish plugin")
        apply(plugin = "signing")
        log("apply signing plugin")
        configureMavenRepository()
        addMissingPublications()
        lazilyConfigurePOM()
        configureSigning()
    }
}
