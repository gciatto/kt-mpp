package io.gciatto.kt.mpp

import org.gradle.api.Project

class MultiPlatformMultiProject : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        if (isRootProject) {
            extensions.create<RootMultiProjectExtension>("multiPlatformMultiProject", this)
        } else {
            val rootExtension = rootProject.extensions.getByType<MutableMultiProjectExtension>()
            extensions.create<MultiProjectExtensionView>("multiPlatformMultiProject", rootExtension)
        }
        log("apply multi-platform-multi-project plugin")
    }
}
