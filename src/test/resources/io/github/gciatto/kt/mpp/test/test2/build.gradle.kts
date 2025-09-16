import io.github.gciatto.kt.mpp.utils.log
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.kotlin.dsl.withType
import org.gradle.api.logging.LogLevel

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("io.github.gciatto.kt-mpp.multiplatform")
    id("io.github.gciatto.kt-mpp.versions")
    id("io.github.gciatto.kt-mpp.documentation")
    id("io.github.gciatto.kt-mpp.maven-publish")
    id("io.github.gciatto.kt-mpp.npm-publish")
}

group = "io.github.gciatto"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs {
            binaries.library()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.reflect)
            }
        }
    }
}

val localMavenRepoDir = rootProject.projectDir.absoluteFile.resolve("build/.m2/repository")
println("Set local Maven repository path: $localMavenRepoDir")
System.setProperty("maven.repo.local", localMavenRepoDir.absolutePath)

plugins.withId("maven-publish") {
    extensions.getByType(PublishingExtension::class.java).run {
        tasks.create("showPublications") {
            doLast {
                log("publications: ${publications.joinToString { it.name }}", LogLevel.LIFECYCLE)
            }
        }
    }
}

//tasks.create("printMavenLocal") {
//    doLast {
//        fileTree(localMavenRepoDir).forEach {
//            println(it)
//        }
//    }
//    val printMavenLocal = this
//    tasks.withType<PublishToMavenLocal> {
//        printMavenLocal.mustRunAfter(this)
//    }
//}
