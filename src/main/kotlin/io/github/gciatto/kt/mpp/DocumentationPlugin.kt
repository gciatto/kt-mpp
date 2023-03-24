package io.github.gciatto.kt.mpp

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.gradle.plugins.signing.Sign

class DocumentationPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val dokka = apply(DokkaPlugin::class)
        log("apply ${dokka::class.java.name} as doc generator")
        tasks.withType(DokkaMultiModuleTask::class.java) {
            tasks.create<Zip>("${it.name}Zip") {
                group = "documentation"
                archiveClassifier.set("documentation")
                from(it.outputDirectory)
                dependsOn(it)
                log("create $path task depending on ${it.path}")
            }
        }
        val suffix = getOptionalProperty("dokkaArtifactInMavenPublication") ?: "Html"
        tasks.withType(DokkaTask::class.java).matching { it.name.contains(suffix, true) }.all {
            crateJavadocTasksFromDokkaTaskForAllPublications(it)
        }
    }

    private fun Project.crateJavadocTasksFromDokkaTaskForAllPublications(dokkaTask: DokkaTask) {
        plugins.withId("maven-publish") {
            configure(PublishingExtension::class) {
                publications.withType(MavenPublication::class.java) {
                    crateJavadocTaskFromDokkaTask(dokkaTask, it)
                }
            }
        }
    }

    private fun Project.crateJavadocTaskFromDokkaTask(dokkaTask: DokkaTask, publication: MavenPublication) {
        createJarTask("${dokkaTask.name}${publication.name.capital()}Jar", "javadoc", "documentation") {
            dependsOn(dokkaTask)
            from(dokkaTask.outputDirectory)
            log("let $path task depend on ${dokkaTask.path}")
            publication.addJarTask(this)
            tasks.withType(Sign::class.java).matching { "Publication" in it.name }.configureEach {
                it.mustRunAfter(this)
                log("let task ${it.path} run after $path")
            }
        }
    }
}
