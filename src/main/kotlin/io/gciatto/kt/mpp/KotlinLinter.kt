package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintIdeaPlugin
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

class KotlinLinter : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply<KtlintPlugin>()
        apply<KtlintIdeaPlugin>()

        configure<KtlintExtension> {
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
