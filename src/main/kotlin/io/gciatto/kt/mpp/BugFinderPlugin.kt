package io.gciatto.kt.mpp

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project

class BugFinderPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val detekt = apply<DetektPlugin>()
        log("apply ${detekt::class.java.name} as bug finder")
        configure<DetektExtension> {
            // toolVersion = "1.19.0"
            config = rootProject.files(".detekt.yml")
            log("configure bug finder from files: ${config.files.joinToString()}")
            buildUponDefaultConfig = true
        }
    }
}
