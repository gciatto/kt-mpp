package io.gciatto.kt.mpp

import Developer.Companion.getAllDevs
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

class PublishOnMaven : AbstractProjectPlugin() {

    lateinit var publishableClassifiers: DomainObjectSet<String>

    private fun Project.configureMavenRepository() {
        configure<PublishingExtension> {
            repositories { repos ->
                repos.maven { maven ->
                    getOptionalProperty("mavenRepo")?.let { maven.url = uri(it) }
                    val mavenUsername: String? = getOptionalProperty("mavenUsername")
                    val mavenPassword: String? = getOptionalProperty("mavenPassword")
                    if (mavenUsername != null && mavenPassword != null) {
                        maven.credentials {
                            it.username = mavenUsername
                            it.password = mavenPassword
                        }
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
                }
                val signAll = project.tasks.create("signAllPublications")
                project.tasks.withType(Sign::class.java) {
                    signAll.dependsOn(this)
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
                    }
                }
                plugins.withId("org.jetbrains.kotlin.js") {
                    publications.maybeCreate("js", MavenPublication::class.java).run {
                        from(components.getAt("kotlin"))
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
                    pub.version = project.version.toString()
                    project.tasks.withType(Jar::class.java) {
                        if ("Html" in name && it.archiveClassifier.getOrElse("") in publishableClassifiers) {
                            pub.artifact(it)
                        }
                    }
                    pub.pom { pom ->
                        pom.name.set(getOptionalProperty("projectLongName"))
                        pom.description.set(getOptionalProperty("projectDescription"))
                        pom.url.set(getOptionalProperty("projectHomepage"))
                        pom.licenses { licenses ->
                            licenses.license {
                                it.name.set(getOptionalProperty("projectLicense"))
                                it.url.set(getOptionalProperty("projectLicenseUrl"))
                            }
                        }
                        pom.developers { devs ->
                            for (dev in project.getAllDevs()) {
                                dev.applyTo(devs)
                            }
                        }
                        pom.scm {
                            it.connection.set(getOptionalProperty("scmConnection"))
                            it.url.set(getOptionalProperty("scmUrl"))
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
