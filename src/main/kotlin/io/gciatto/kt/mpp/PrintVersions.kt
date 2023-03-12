package io.gciatto.kt.mpp

import org.gradle.api.Project

class PrintVersions : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val versioning = "versioning"
        tasks.register("printVersion") {
            it.group = versioning
            it.doLast { println(project.version) }
        }
        tasks.register("printNpmVersion") {
            it.group = versioning
            it.doLast { println(project.npmCompliantVersion) }
        }
        log("apply print-versions plugin")
    }
}
