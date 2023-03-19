package io.github.gciatto.kt.mpp

typealias ProjectConfiguration = Set<PluginDescriptor<*>>

val baseProject: ProjectConfiguration = buildSet {
    add(Plugins.documentation)
    add(Plugins.linter)
    add(Plugins.bugFinder)
    add(Plugins.versions)
}

val defaultKtProject: ProjectConfiguration = buildSet {
    add(Plugins.multiplatform)
    addAll(baseProject)
}

val defaultJvmProject: ProjectConfiguration = buildSet {
    add(Plugins.jvmOnly)
    addAll(baseProject)
}

val defaultJsProject: ProjectConfiguration = buildSet {
    add(Plugins.jsOnly)
    addAll(baseProject)
}

val defaultOtherProject: ProjectConfiguration = buildSet {
    add(Plugins.versions)
}
