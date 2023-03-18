package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

class VersionsPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val versioning = "versioning"
        group = rootProject.group
        log("copy group from rootProject: $group")
        version = rootProject.version
        log("copy version from rootProject: $version")
        tasks.register("printVersion") {
            it.group = versioning
            it.doLast { println(project.version) }
        }
        tasks.register("printNpmVersion") {
            it.group = versioning
            it.doLast { println(project.npmCompliantVersion) }
        }
        log("apply ${Plugins.versions.name} plugin")
        afterEvaluate {
            plugins.withId("maven-publish") {
                configure(PublishingExtension::class) {
                    publications.withType(MavenPublication::class.java) {
                        it.copyMavenGroupAndVersionFromProject()
                    }
                }
            }
        }
    }
}
