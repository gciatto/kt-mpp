package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class JvmOnlyPlugin : AbstractKotlinProjectPlugin("jvm") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        apply(plugin = "java-library")
        log("apply java-library plugin")
        tasks.withType(KotlinCompile::class.java) { task ->
            task.kotlinOptions {
                configureKotlinOptions(task.name)
                configureJvmKotlinOptions(task.name)
            }
        }
        dependencies {
            addMainDependencies(project, target = "jdk8")
            addTestDependencies(project, target = "junit")
        }
        configure(JavaPluginExtension::class) {
            withSourcesJar()
            log("configure JVM library to include sources JAR")
        }
        addTaskAliases()
    }
}
