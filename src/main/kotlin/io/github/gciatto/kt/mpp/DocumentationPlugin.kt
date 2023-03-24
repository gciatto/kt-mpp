package io.github.gciatto.kt.mpp

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create

class DocumentationPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val dokka = apply(DokkaPlugin::class)
        log("apply ${dokka::class.java.name} as doc generator")
        tasks.withType(DokkaMultiModuleTask::class.java) {
            val task = tasks.create<ZipDokkaTask>("${it.name}Zip", it)
            log("create ${task.path} task depending on ${it.path}")
        }
        plugins.withId("maven-publish") {
            configure(PublishingExtension::class) {
                publications.withType(MavenPublication::class.java) { pub ->
                    val suffix = getOptionalProperty("dokkaArtifactInMavenPublication") ?: "Html"
                    tasks.withType(DokkaTask::class.java).matching { it.name.contains(suffix, true) }
                        .all { dokkaTask ->
                            val javadocJarTaskName = "${dokkaTask.name}${pub.name.capital()}Jar"
                            val javadocJarTask = tasks.create<JarDokkaTask>(javadocJarTaskName, dokkaTask)
                            log("create ${javadocJarTask.path} task depending on ${dokkaTask.path}")
                            tasks.matching { it.name == "assemble" }.configureEach { assemble ->
                                assemble.dependsOn(javadocJarTask)
                                log("make ${assemble.path} task dependant on ${javadocJarTask.path}")
                            }
                        }
                }
            }
        }
    }
}
