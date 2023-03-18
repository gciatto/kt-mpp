package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

enum class ProjectType {
    KOTLIN, JVM, JS, OTHER
}

sealed interface MultiProjectExtension {
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

sealed interface MutableMultiProjectExtension : MultiProjectExtension {
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

internal open class RootMultiProjectExtension(project: Project) : MutableMultiProjectExtension {

    private val rootProject: Project = project.rootProject

    override var defaultProjectType: ProjectType = ProjectType.KOTLIN

    private var ktProjectsCache: Set<Project>? = null

    override var ktProjects: Set<Project>
        get() = ktProjectsCache ?: selectKtProjects()
        set(value) {
            ktProjectsCache = value
        }

    private fun selectKtProjects(): Set<Project> =
        selectProjects(ProjectType.KOTLIN, jvmProjects, jsProjects, otherProjects)

    private var jvmProjectsCache: Set<Project>? = null

    override var jvmProjects: Set<Project>
        get() = jvmProjectsCache ?: selectJvmProjects()
        set(value) {
            jvmProjectsCache = value
        }

    private fun selectJvmProjects(): Set<Project> =
        selectProjects(ProjectType.JVM, ktProjects, jsProjects, otherProjects)

    private var jsProjectsCache: Set<Project>? = null

    override var jsProjects: Set<Project>
        get() = jsProjectsCache ?: selectJsProjects()
        set(value) {
            jsProjectsCache = value
        }

    private fun selectJsProjects(): Set<Project> =
        selectProjects(ProjectType.JS, ktProjects, jvmProjects, otherProjects)

    private var otherProjectsCache: Set<Project>? = null

    override var otherProjects: Set<Project>
        get() = otherProjectsCache ?: selectOtherProjects()
        set(value) {
            otherProjectsCache = value
        }

    override fun ktProjects(identifier: String, vararg other: String) {
        ktProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun jvmProjects(identifier: String, vararg other: String) {
        jvmProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun jsProjects(identifier: String, vararg other: String) {
        jsProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    override fun otherProjects(identifier: String, vararg other: String) {
        otherProjects = selectProjectsByNameOrPath(identifier, *other)
    }

    private fun selectOtherProjects(): Set<Project> =
        selectProjects(ProjectType.OTHER, ktProjects, jvmProjects, jsProjects)

    private fun selectProjects(default: ProjectType, vararg skippable: Set<Project>) =
        if (defaultProjectType == default) {
            val toSkip = skippable.asSequence().flatMap { it.asSequence() }.toSet()
            rootProject.allProjects.filter { it !in toSkip }.toSet()
        } else {
            emptySet()
        }

    private fun selectProjectsByNameOrPath(identifier: String, vararg others: String): Set<Project> =
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

    companion object {
        private val Project.allProjects: Sequence<Project>
            get() = sequence {
                yield(this@allProjects)
                for (subproject in subprojects) {
                    yieldAll(subproject.allProjects)
                }
            }
    }
}

internal open class MultiProjectExtensionView(delegate: MultiProjectExtension) :
    MultiProjectExtension by delegate
