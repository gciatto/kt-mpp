package io.github.gciatto.kt.mpp.jar

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import io.github.gciatto.kt.mpp.AbstractProjectPlugin
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class FatJarPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        apply<ShadowPlugin>()
        log("apply ${ShadowPlugin::class.java.name} plugin")
        shadowJarTask()
        multiPlatformHelper.fatJarPlatforms.all {
            shadowJarTask(platform = it)
        }
    }
}
