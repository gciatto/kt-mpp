package io.github.gciatto.kt.mpp.verification

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

class LinterPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply(KtlintPlugin::class)
        configure(KtlintExtension::class) {
            // version.set("0.22.0")
            debug.set(false)
            verbose.set(true)
            android.set(false)
            outputToConsole.set(true)
            outputColorName.set("RED")
            ignoreFailures.set(false)
            enableExperimentalRules.set(true)
            reporters {
                it.reporter(ReporterType.PLAIN)
                it.reporter(ReporterType.CHECKSTYLE)
            }
            filter {
                it.exclude("**/generated/**")
                it.include("**/kotlin/**")
            }
        }
    }
}
