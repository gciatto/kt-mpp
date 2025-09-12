package io.github.gciatto.kt.mpp.publishing

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.utils.getAsEitherFileOrValue
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
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
    context(p: Project)
    private fun Repository.configure(
        username: String?,
        pwd: String?,
    ) {
        if (username != null && pwd != null) {
            user.set(username)
            password.set(pwd)
            @Suppress("ktlint")
            p.log(
                "configure Maven repository $name " +
                    "(URL: $url, username: ${user.get().asField()}, " +
                    "password: ${password.get().asPassword()})"
            )
        }
    }

    private fun Project.configureMavenRepositories() =
        configure(PublishOnCentralExtension::class) {
            // configureMavenCentral.set(true)
            // mavenCentral.run {
            //    val mavenCentralUsername: String? = multiPlatformHelper.mavenCentralUsername.orNull
            //    val mavenCentralPassword: String? = multiPlatformHelper.mavenCentralPassword.orNull
            //    configure(mavenCentralUsername, mavenCentralPassword)
            // }
            multiPlatformHelper.otherMavenRepo.orNull?.takeIf { "oss.sonatype.org" !in it.host }?.let {
                val mavenUsername: String? = multiPlatformHelper.otherMavenUsername.orNull
                val mavenPassword: String? = multiPlatformHelper.otherMavenPassword.orNull
                repository(it.toString()) {
                    user.set(mavenUsername)
                    password.set(mavenPassword)
                    @Suppress("ktlint")
                log(
                    "configure Maven repository $name " +
                        "(URL: $it, username: ${user.get().asField()}, " +
                        "password: ${password.get().asPassword()})"
                )
                }
            }
        }

    @Suppress("UnsafeCallOnNullableType")
    private fun Project.configureSigning() =
        configure(SigningExtension::class) {
            val signingKey: String? = multiPlatformHelper.signingKey.orNull
            val signingPassword: String? = multiPlatformHelper.signingPassword.orNull
            if (arrayOf(signingKey, signingPassword).none { it.isNullOrBlank() }) {
                val actualKey = signingKey!!.getAsEitherFileOrValue(project)
                val actualPassphrase = signingPassword!!.getAsEitherFileOrValue(project)
                log(
                    "configure signatory for publication for project $name: " +
                        "key=${actualKey.asPassword()}, passphrase=${actualPassphrase.asPassword()}",
                )
                useInMemoryPgpKeys(actualKey, actualPassphrase)
            } else {
                @Suppress("ktlint")
            log(
                "one property in {signingKey, signingPassword} is unset or blank, " +
                    "hence Maven publications won't be signed"
            )
            }
            val signAll = tasks.create("signAllPublications") { it.group = "signing" }
            tasks.withType(Sign::class.java) {
                it.group = "signing"
                signAll.dependsOn(it)
                log("make ${signAll.path} tasks dependant on ${it.path}")
            }
        }

    private fun Project.configurePublications() =
        configure(PublishOnCentralExtension::class) {
            val mpp = multiPlatformHelper
            projectLongName.set(mpp.projectLongName.getLogging("set POM name: %s"))
            projectDescription.set(mpp.projectDescription.getLogging("set POM description: %s"))
            repoOwner.set(mpp.repoOwner.getLogging("set repoOwner: %s"))
            projectUrl.set(mpp.projectHomepage.asStringLogging("set POM URL: %s"))
            licenseName.set(mpp.projectLicense.getLogging("set POM license name: %s"))
            licenseUrl.set(mpp.projectLicenseUrl.asStringLogging("set POM license URL: %s"))
            scmConnection.set(mpp.scmConnection.getLogging("add POM SCM connection: %s"))
            addMissingInformationToPublications()
        }

    private fun Project.addMissingInformationToPublications() =
        configure(PublishingExtension::class) {
            publications.withType(MavenPublication::class.java) { pub ->
                pub.pom { pom ->
                    pom.developers { devs ->
                        multiPlatformHelper.developers.all {
                            it.applyTo(devs)
                            log("add POM developer for publication ${pub.name}: $it")
                        }
                    }
                    pom.scm { scm ->
                        val logMsg = "set POM SCM URL for publication ${pub.name}: %s"
                        scm.url.set(multiPlatformHelper.scmUrl.asStringLogging(logMsg))
                    }
                    pom.issueManagement { issues ->
                        issues.url.set(multiPlatformHelper.issuesUrl.asStringLogging("set POM issues URL to %s"))
                    }
                }
            }
        }

//    private fun Project.configurePublishOnCentralExtension() =
//        configure(PublishOnCentralExtension::class) {
//            // autoConfigureAllPublications.set(true)
//        }

//    private fun Project.fixSignPublishTaskDependencies() {
//        tasks.withType(Sign::class.java) { before ->
//            tasks.withType(AbstractPublishToMaven::class.java) { after ->
//                after.mustRunAfter(before)
//                log("make task ${after.path} run after ${before.path}")
//            }
//        }
//    }

//    private fun Project.fixMavenPublicationsJavadocArtifact() {
//        plugins.withType(PublishOnMavenPlugin::class.java) { _ ->
//            configure(PublishOnCentralExtension::class) {
//                val logMsg = "use %s style for javadoc JAR when publishing"
//                // docStyle.set(multiPlatformHelper.docStyle.getLogging(logMsg))
//                configure(PublishingExtension::class) {
//                    publications.withType(MavenPublication::class.java).matching { "OSSRH" !in it.name }.all {
//                        it.artifact(tasks.named("javadocJar"))
//                        log("add javadoc JAR to publication ${it.name}")
//                    }
//                }
//            }
//        }
//    }

    @Suppress("UNUSED_PARAMETER")
    override fun Project.applyThisPlugin() {
        fun configurePlugin(plugin: Plugin<*>) {
            apply(plugin = "org.danilopianini.publish-on-central")
            log("apply org.danilopianini.publish-on-central plugin")
            multiPlatformHelper.initializeMavenRelatedProperties()
            // configurePublishOnCentralExtension()
            configureMavenRepositories()
            configurePublications()
            addMissingInformationToPublications()
            configureSigning()
            // fixSignPublishTaskDependencies()
            // fixMavenPublicationsJavadocArtifact()
        }
        forAllKotlinPlugins { configurePlugin(it) }
    }
}
