package io.github.gciatto.kt.mpp

import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import javax.inject.Inject

open class JarDokkaTask @Inject constructor(dependingOn: DokkaTask) : Jar() {
    init {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(dependingOn.outputDirectory)
        dependsOn(dependingOn)
    }
}
