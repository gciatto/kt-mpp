package io.github.gciatto.kt.mpp.documentation

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.utils.log
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask // New base task class

class DocumentationPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        forAllKotlinPlugins { _ ->
            apply<DokkaPlugin>()
            log("Applied Dokka v2 plugin")

            tasks.withType<DokkaGenerateTask>().all { dokkaTask ->
                val zipTaskName = "${dokkaTask.name}Zip"

                tasks.register<Zip>(zipTaskName) {
                    group = "documentation"
                    archiveClassifier.set("documentation")

                    from(dokkaTask.outputDirectory)
                    dependsOn(dokkaTask)
                    doFirst {
                        log("Creating $path task depending on ${dokkaTask.path}")
                    }
                }
            }
        }
    }
}
