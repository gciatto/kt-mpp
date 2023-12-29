package io.github.gciatto.kt.mpp.helpers

import io.github.gciatto.kt.mpp.ProjectConfiguration
import org.gradle.api.Project

interface MutableMultiProjectExtension : MultiProjectExtension {
    override var defaultProjectType: ProjectType

    override var ktProjects: Set<Project>

    override var jvmProjects: Set<Project>

    override var jsProjects: Set<Project>

    override var otherProjects: Set<Project>

    fun ktProjects(identifier: String, vararg other: String)

    fun jvmProjects(identifier: String, vararg other: String)

    fun jsProjects(identifier: String, vararg other: String)

    fun otherProjects(identifier: String, vararg other: String)

    var ktProjectTemplate: ProjectConfiguration

    var jvmProjectTemplate: ProjectConfiguration

    var jsProjectTemplate: ProjectConfiguration

    var otherProjectTemplate: ProjectConfiguration

    fun applyProjectTemplates()
}
