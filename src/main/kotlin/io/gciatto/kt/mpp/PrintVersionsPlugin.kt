package io.gciatto.kt.mpp

import org.gradle.api.Project

class PrintVersionsPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val versioning = "versioning"
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
    }
}
