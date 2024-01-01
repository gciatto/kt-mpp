package io.github.gciatto.kt.mpp.verification

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project

class BugFinderPlugin : AbstractProjectPlugin() {
    companion object {
        private val PATTERN_DETEKT_TASK_NAME = Regex("^detekt(Jvm|Js)?(Main|Test)?$")
    }

    private val Project.detektTasks: DomainObjectCollection<Detekt>
        get() = tasks.withType(Detekt::class.java).matching {
            it.name.matches(PATTERN_DETEKT_TASK_NAME)
        }

    override fun Project.applyThisPlugin() {
        val detekt = apply(DetektPlugin::class)
        log("apply ${detekt::class.java.name} as bug finder")
        multiPlatformHelper.initializeBugFinderRelatedProperties()
        configure(DetektExtension::class) {
            multiPlatformHelper.bugFinderConfig.let {
                if (!it.isEmpty) {
                    config.setFrom(it)
                    log("configure bug finder from files: ${config.files.joinToString()}")
                }
            }
            buildUponDefaultConfig = true
        }
        val detektAll = tasks.maybeCreate("detektAll").also { it.group = "verification" }
        detektTasks.all { detektAll.dependsOn(it) }
        tasks.matching { it.name == "check" }.all {
            it.dependsOn(detektAll)
            log("add task ${detektAll.path} and make ${it.path} depend on it")
        }
    }
}
