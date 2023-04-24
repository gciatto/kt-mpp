package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin

class DocumentationPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        forAllKotlinPlugins { _ ->
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
        }
    }
}
