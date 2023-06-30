package io.github.gciatto.kt.mpp

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project

class BugFinderPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        val detekt = apply(DetektPlugin::class)
        log("apply ${detekt::class.java.name} as bug finder")
        configure(DetektExtension::class) {
            // toolVersion = "1.19.0"
            config.setFrom(rootProject.files(".detekt.yml"))
            log("configure bug finder from files: ${config.files.joinToString()}")
            buildUponDefaultConfig = true
        }
    }
}
