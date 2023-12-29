package io.github.gciatto.kt.mpp.helpers

import org.gradle.api.Project

interface MultiProjectExtension {
    val defaultProjectType: ProjectType

    val ktProjects: Set<Project>

    val jvmProjects: Set<Project>

    val jsProjects: Set<Project>

    val otherProjects: Set<Project>

    fun Iterable<Project>.except(names: Iterable<String>): Set<Project> =
        filter { it.name !in names }.toSet()

    fun Iterable<Project>.except(first: String, vararg others: String): Set<Project> {
        val names = setOf(first, *others)
        return except(names)
    }
}
