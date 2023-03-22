import io.github.gciatto.kt.mpp.ProjectType

plugins {
    id("io.github.gciatto.kt-mpp.multi-project-helper")
}

group = "io.github.gciatto.example"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    jvmProjects(":subproject-jvm")
    jsProjects(":subproject-js")
    otherProjects(":subproject-other")
    applyProjectTemplates()
}

val localMavenRepoDir = rootProject.projectDir.absoluteFile.resolve("build/.m2/repository")
println("Local Maven Repository: $localMavenRepoDir")

allprojects {
    repositories {
        mavenCentral()
    }
    plugins.withId("maven-publish") {
        extensions.getByType(PublishingExtension::class.java).run {
            publications {
                configureEach {
                    repositories {
                        mavenLocal {
                            url = localMavenRepoDir.toURI()
                        }
                    }
                }
            }
        }
    }
}
