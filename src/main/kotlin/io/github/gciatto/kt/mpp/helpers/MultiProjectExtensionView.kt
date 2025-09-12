package io.github.gciatto.kt.mpp.helpers

internal open class MultiProjectExtensionView(
    delegate: MultiProjectExtension,
) : MultiProjectExtension by delegate
