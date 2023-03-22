package io.github.gciatto.kt.mpp

import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

class DocumentationPlugin : AbstractProjectPlugin() {
    private inline fun <reified A : Zip> Project.createJavadocArchiveTask(dependingOn: AbstractDokkaTask): A {
        return tasks.create("${dependingOn.name}${A::class.simpleName}", A::class.java) {
            it.group = "documentation"
            it.archiveClassifier.set("javadoc")
            it.from(dependingOn.outputDirectory)
            it.dependsOn(dependingOn)
        }
    }
    override fun Project.applyThisPlugin() {
        val dokka = apply(DokkaPlugin::class)
        log("apply ${dokka::class.java.name} as doc generator")
        tasks.withType(DokkaMultiModuleTask::class.java) {
            val task = createJavadocArchiveTask<Zip>(dependingOn = it)
            log("create ${task.path} task depending on ${it.path}")
        }
        tasks.withType(DokkaTask::class.java) {
            val task = createJavadocArchiveTask<Jar>(dependingOn = it)
            log("create ${task.path} task depending on ${it.path}")
            tasks.named("assemble").configure { assemble ->
                assemble.dependsOn(it)
                log("make ${assemble.path} task dependant on ${it.path}")
            }
        }
    }
}
