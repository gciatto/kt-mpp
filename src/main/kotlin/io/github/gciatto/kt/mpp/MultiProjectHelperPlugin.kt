package io.github.gciatto.kt.mpp

import org.gradle.api.Project

class MultiProjectHelperPlugin : AbstractProjectPlugin() {
    override fun Project.applyThisPlugin() {
        if (isRootProject) {
            extensions.create("multiProjectHelper", RootMultiProjectExtension::class, this)
        } else {
            val rootExtension = rootProject.extensions.getByType(MutableMultiProjectExtension::class)
            extensions.create("multiProjectHelper", MultiProjectExtensionView::class, rootExtension)
        }
        log("apply ${Plugins.multiProjectHelper.name} plugin")
    }
}
