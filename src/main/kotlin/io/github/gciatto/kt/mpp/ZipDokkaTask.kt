package io.github.gciatto.kt.mpp

import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import javax.inject.Inject

open class ZipDokkaTask @Inject constructor(dependingOn: DokkaMultiModuleTask) : Zip() {
    init {
        group = "documentation"
        archiveClassifier.set("doc")
        from(dependingOn.outputDirectory)
        dependsOn(dependingOn)
    }
}
