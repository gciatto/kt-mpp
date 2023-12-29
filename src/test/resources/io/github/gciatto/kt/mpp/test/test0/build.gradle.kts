import io.github.gciatto.kt.mpp.helpers.ProjectType
import io.github.gciatto.kt.mpp.utils.log
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.kotlin.dsl.withType
import org.gradle.api.logging.LogLevel

plugins {
    id("io.github.gciatto.kt-mpp.multi-project-helper")
}

group = "io.github.gciatto"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    jvmProjects(":subproject-jvm")
    jsProjects(":subproject-js")
    otherProjects(":subproject-other")
    applyProjectTemplates()
}

val localMavenRepoDir = rootProject.projectDir.absoluteFile.resolve("build/.m2/repository")
println("Set local Maven repository path: $localMavenRepoDir")
System.setProperty("maven.repo.local", localMavenRepoDir.absolutePath)

allprojects {
    repositories {
        mavenCentral()
    }
    plugins.withId("maven-publish") {
        extensions.getByType(PublishingExtension::class.java).run {
            tasks.create("showPublications") {
                doLast {
                    log("publications: ${publications.joinToString { it.name }}", LogLevel.LIFECYCLE)
                }
            }
        }
    }
}

tasks.create("printMavenLocal") {
    doLast {
        fileTree(localMavenRepoDir).forEach {
            println(it)
        }
    }
    val printMavenLocal = this
    tasks.withType<PublishToMavenLocal> {
        printMavenLocal.mustRunAfter(this)
    }
}
