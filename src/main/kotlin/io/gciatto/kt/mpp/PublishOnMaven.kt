package io.gciatto.kt.mpp

import io.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

class PublishOnMaven : AbstractProjectPlugin() {

    @Suppress("MemberVisibilityCanBePrivate")
    lateinit var publishableClassifiers: DomainObjectSet<String>

    private fun Project.configureMavenRepository() {
        configure<PublishingExtension> {
            repositories { repos ->
                repos.maven { maven ->
                    getOptionalProperty("mavenRepo")?.let {
                        maven.url = uri(it)
                    } ?: log("property mavenRepo unset", LogLevel.WARN)
                    val mavenUsername: String? = getOptionalProperty("mavenUsername")
                    val mavenPassword: String? = getOptionalProperty("mavenPassword")
                    if (mavenUsername != null && mavenPassword != null) {
                        maven.credentials {
                            it.username = mavenUsername
                            it.password = mavenPassword
                        }
                        log("configure Maven repository ${maven.name} " +
                                "(URL: ${maven.url}, username: ${maven.credentials.username}, " +
                                "password: ${"".padEnd(maven.credentials.password?.length ?: 0, '*')})")
                    } else {
                        log("configure Maven repository ${maven.name} with no credentials", LogLevel.WARN)
                    }
                }
            }
        }
    }

    private fun Project.configureSigning() {
        configure<PublishingExtension> {
            configure<SigningExtension> {
                val signingKey: String? = getOptionalProperty("signingKey")
                val signingPassword: String? = getOptionalProperty("signingPassword")
                if (arrayOf(signingKey, signingPassword).none { it.isNullOrBlank() }) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign(publications)
                    log("configure signing for publications: ${publications.names.joinToString()}")
                } else {
                    log("one property in {signingKey, signingPassword} is unset, " +
                            "hence Maven publications won't be signed")
                }
                val signAll = project.tasks.create("signAllPublications")
                project.tasks.withType(Sign::class.java) {
                    signAll.dependsOn(it)
                    log("create signAllPublications tasks depending on ${it.path}")
                }
            }
        }
    }

    private fun Project.addMissingPublications() {
        configure<PublishingExtension> {
            configure<SigningExtension> {
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

    private fun Project.lazilyConfigurePOM() {
        configure<PublishingExtension> {
            afterEvaluate { project ->
                publications.withType(MavenPublication::class.java) { pub ->
                    pub.groupId = project.group.toString()
                    log("set groupId of publication ${pub.name}: ${pub.groupId}")
                    pub.version = project.version.toString()
                    log("set version of publication ${pub.name}: ${pub.version}")
                    project.tasks.withType(Jar::class.java) {
                        if ("Html" in name && it.archiveClassifier.getOrElse("") in publishableClassifiers) {
                            pub.artifact(it)
                            log("add artifact to publication ${pub.name}: ${it.archiveFileName.get()}")
                        }
                    }
                    pub.pom { pom ->
                        getOptionalProperty("projectLongName")?.let {
                            pom.name.set(it)
                            log("set POM name in publication ${pub.name}: $it")
                        } ?: log("property projectLongName unset", LogLevel.WARN)
                        getOptionalProperty("projectDescription")?.let {
                            pom.description.set(it)
                            log("set POM description in publication ${pub.name}: $it")
                        } ?: log("property projectDescription unset", LogLevel.WARN)
                        getOptionalProperty("projectHomepage")?.let {
                            pom.url.set(it)
                            log("set POM URL in publication ${pub.name}: $it")
                        } ?: log("property projectHomepage unset", LogLevel.WARN)
                        pom.licenses { licenses ->
                            licenses.license { license ->
                                getOptionalProperty("projectLicense")?.let {
                                    license.name.set(it)
                                    log("add POM license in publication ${pub.name}: $it")
                                } ?: log("property projectLicense unset", LogLevel.WARN)
                                getOptionalProperty("projectLicenseUrl")?.let {
                                    license.url.set(it)
                                    log("add POM license URL in publication ${pub.name}: $it")
                                } ?: log("property projectLicenseUrl unset", LogLevel.WARN)
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
                            } ?: log("property scmConnection unset", LogLevel.WARN)
                            getOptionalProperty("scmUrl")?.let {
                                scm.url.set(it)
                                log("add POM SCM URL in publication ${pub.name}: $it")
                            } ?: log("property scmUrl unset", LogLevel.WARN)
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
