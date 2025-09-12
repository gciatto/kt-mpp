package io.github.gciatto.kt.mpp.helpers

import io.github.gciatto.kt.mpp.ProjectConfiguration
import io.github.gciatto.kt.mpp.defaultJsProject
import io.github.gciatto.kt.mpp.defaultJvmProject
import io.github.gciatto.kt.mpp.defaultKtProject
import io.github.gciatto.kt.mpp.defaultOtherProject
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

internal open class RootMultiProjectExtension(
    project: Project,
) : MutableMultiProjectExtension {
    private val rootProject: Project = project.rootProject

    override var defaultProjectType: ProjectType = ProjectType.KOTLIN

    private var ktProjectsCache: Set<Project>? = null

    override var ktProjects: Set<Project>
        get() = ktProjectsCache ?: selectKtProjects()
        set(value) {
            ktProjectsCache = value
        }

    private fun selectKtProjects(): Set<Project> =
        selectProjects(ProjectType.KOTLIN, jvmProjectsCache, jsProjectsCache, otherProjectsCache)

    private var jvmProjectsCache: Set<Project>? = null

    override var jvmProjects: Set<Project>
        get() = jvmProjectsCache ?: selectJvmProjects()
        set(value) {
            jvmProjectsCache = value
        }

    private fun selectJvmProjects(): Set<Project> =
        selectProjects(ProjectType.JVM, ktProjectsCache, jsProjectsCache, otherProjectsCache)

    private var jsProjectsCache: Set<Project>? = null

    override var jsProjects: Set<Project>
        get() = jsProjectsCache ?: selectJsProjects()
        set(value) {
            jsProjectsCache = value
        }

    private fun selectJsProjects(): Set<Project> =
        selectProjects(ProjectType.JS, ktProjectsCache, jvmProjectsCache, otherProjectsCache)

    private var otherProjectsCache: Set<Project>? = null

    override var otherProjects: Set<Project>
        get() = otherProjectsCache ?: selectOtherProjects()
        set(value) {
            otherProjectsCache = value
        }

    override fun ktProjects(
        identifier: String,
        vararg other: String,
    ) {
        ktProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun jvmProjects(
        identifier: String,
        vararg other: String,
    ) {
        jvmProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun jsProjects(
        identifier: String,
        vararg other: String,
    ) {
        jsProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun otherProjects(
        identifier: String,
        vararg other: String,
    ) {
        otherProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    private fun selectOtherProjects(): Set<Project> =
        selectProjects(ProjectType.OTHER, ktProjectsCache, jvmProjectsCache, jsProjectsCache)

    private fun selectProjects(
        default: ProjectType,
        vararg skippable: Set<Project>?,
    ) = if (defaultProjectType == default) {
        val toSkip =
            skippable
                .asSequence()
                .filterNotNull()
                .flatMap { it.asSequence() }
                .toSet()
        rootProject.allProjects.filter { it !in toSkip }.toSet()
    } else {
        emptySet()
    }

    private fun selectProjectsByNameOrPath(
        identifier: String,
        vararg others: String,
    ): Set<Project> =
        sequenceOf(identifier, *others)
            .map { selectProjectByNameOrPath(it) ?: error("No such a project: $it") }
            .toSet()

    private fun selectProjectByNameOrPath(identifier: String): Project? =
        rootProject.allProjects.filter { it.name == identifier || it.path == identifier }.firstOrNull()

    override var ktProjectTemplate: ProjectConfiguration = defaultKtProject

    override var jvmProjectTemplate: ProjectConfiguration = defaultJvmProject

    override var jsProjectTemplate: ProjectConfiguration = defaultJsProject

    override var otherProjectTemplate: ProjectConfiguration = defaultOtherProject

    override fun applyProjectTemplates() {
        autoAssignProjectsIfNecessary()
        val projectSets = listOf(ktProjects, jvmProjects, jsProjects, otherProjects)
        val projectTemplates = listOf(ktProjectTemplate, jvmProjectTemplate, jsProjectTemplate, otherProjectTemplate)
        for ((projectSet, template) in projectSets.zip(projectTemplates)) {
            for (project in projectSet) {
                for (plugin in template) {
                    project.apply(plugin = plugin.name)
                }
            }
        }
    }

    private fun autoAssignProjectsIfNecessary() {
        if (listOf(ktProjectsCache, jvmProjectsCache, jsProjectsCache, otherProjectsCache).all { it == null }) {
            val allProjects = rootProject.allProjects.toSet()
            when (defaultProjectType) {
                ProjectType.KOTLIN -> ktProjects = allProjects
                ProjectType.JVM -> jvmProjects = allProjects
                ProjectType.JS -> jsProjects = allProjects
                ProjectType.OTHER -> otherProjects = allProjects
            }
        }
    }

    companion object {
        private val Project.allProjects: Sequence<Project>
            get() =
                sequence {
                    yield(this@allProjects)
                    for (subproject in subprojects) {
                        yieldAll(subproject.allProjects)
                    }
                }
    }
}
