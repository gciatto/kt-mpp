package io.gciatto.kt.mpp

typealias ProjectConfiguration = Set<PluginDescriptor<*>>

val baseProject: ProjectConfiguration = buildSet {
    add(Plugins.kotlinDoc)
    add(Plugins.kotlinLinter)
    add(Plugins.kotlinBugFinder)
}

val defaultKtProject: ProjectConfiguration = buildSet {
    add(Plugins.kotlinMpp)
    addAll(baseProject)
}

val defaultJvmProject: ProjectConfiguration = buildSet {
    add(Plugins.kotlinJvmOnly)
    addAll(baseProject)
}

val defaultJsProject: ProjectConfiguration = buildSet {
    add(Plugins.kotlinJsOnly)
    addAll(baseProject)
}

val defaultOtherProject: ProjectConfiguration = emptySet()
