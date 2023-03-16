package io.gciatto.kt.mpp

import org.gradle.api.Project

class MultiProjectHelperPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        if (isRootProject) {
            extensions.create<RootMultiProjectExtension>("multiProjectHelper", this)
        } else {
            val rootExtension = rootProject.extensions.getByType<MutableMultiProjectExtension>()
            extensions.create<MultiProjectExtensionView>("multiProjectHelper", rootExtension)
        }
        log("apply ${Plugins.multiProjectHelper.name} plugin")
    }
}
