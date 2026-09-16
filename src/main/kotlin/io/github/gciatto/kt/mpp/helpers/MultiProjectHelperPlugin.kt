package io.github.gciatto.kt.mpp.helpers

import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.Plugins
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class MultiProjectHelperPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        if (isRootProject) {
            extensions.create("multiProjectHelper", RootMultiProjectExtension::class, this)
        } else {
            val rootExtension = rootProject.extensions.getByType(MutableMultiProjectExtension::class)
            extensions.create("multiProjectHelper", MultiProjectExtensionView::class, rootExtension)
        }
        log("apply ${Plugins.multiProjectHelper.name} plugin")
        if (multiPlatformHelper.showTestsInConsole.get()) {
            tasks.withType<Test>().configureEach { task ->
                task.testLogging {
                    it.events("passed", "skipped", "failed", "standardOut", "standardError")
                    it.showStandardStreams = true
                    log("Enabled logging for ${task.path}")
                }
            }
        }
    }
}
