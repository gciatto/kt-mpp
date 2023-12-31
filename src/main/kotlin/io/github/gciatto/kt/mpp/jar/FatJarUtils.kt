package io.github.gciatto.kt.mpp.jar

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.gciatto.kt.mpp.helpers.MultiPlatformHelperExtension
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.multiPlatformHelper
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.attributes
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

fun String.toPascalCase(separators: Set<Char> = setOf('_', '-')) =
    split(*separators.toCharArray()).joinToString("") {
        it.capitalized()
    }

private val Project.supportedPlatforms: Set<String>
    get() = multiPlatformHelper.fatJarPlatforms.toSet()

internal fun Project.includedPlatformsFor(platform: String): Set<String> =
    multiPlatformHelper.fatJarPlatformInclusions.filter { it.first == platform }.map { it.second }.toSet() + platform

internal fun Project.excludedPlatformsFor(platform: String): Set<String> =
    supportedPlatforms - includedPlatformsFor(platform)

private fun defaultTaskName(platform: String?): String =
    "shadowJar" + platform?.let { "For${it.toPascalCase()}" }.orEmpty()

private fun Project.defaultExcludedPlatforms(platform: String?): Set<String> =
    platform?.let { excludedPlatformsFor(it) }.orEmpty()

fun MultiPlatformHelperExtension.javaFxFatJars() {
    fatJarPlatforms.addAll(listOf("win", "linux", "mac", "mac-aarch64"))
    fatJarPlatformInclude("mac", "linux")
    fatJarPlatformInclude("mac-aarch64", "linux")
}

fun Project.shadowJarTask(
    platform: String?,
    entryPoint: String?,
    classifier: String?,
    name: String = defaultTaskName(platform),
    excludedPlatforms: Set<String> = defaultExcludedPlatforms(platform),
) = shadowJarTask(platform, provider { entryPoint }, name, provider { classifier }, excludedPlatforms)

@Suppress("NAME_SHADOWING")
fun Project.shadowJarTask(
    platform: String? = null,
    entryPoint: Provider<String> = provider { null },
    name: String = defaultTaskName(platform),
    classifier: Provider<String> = provider { null },
    excludedPlatforms: Set<String> = defaultExcludedPlatforms(platform),
): ShadowJar {
    val entryPoint = entryPoint.orElse(multiPlatformHelper.fatJarEntryPoint)
    val classifier = classifier.orElse(multiPlatformHelper.fatJarClassifier)
    fun setOfFileSystemLocationsToFileTree(locations: Set<FileSystemLocation>): FileTree =
        locations.map { it.asFile }.map { if (it.isDirectory) fileTree(it) else zipTree(it) }.reduce(FileTree::plus)

    fun fileShouldBeIncluded(file: File): Boolean = excludedPlatforms.none { file.name.endsWith("$it.jar") }
    return tasks.maybeCreate(name, ShadowJar::class.java).also { jarTask ->
        jarTask.group = "shadow"
        entryPoint.orNull?.let { className ->
            jarTask.manifest { it.attributes("Main-Class" to className) }
        }
        jarTask.archiveBaseName.set(project.provider { "${rootProject.name}-${project.name}" })
        jarTask.archiveVersion.set(project.provider { project.version.toString() })
        jarTask.archiveClassifier.set(platform?.let { p -> classifier.map { "$it-$p" } } ?: classifier)
        configureJarForProject(jarTask, ::fileShouldBeIncluded, ::setOfFileSystemLocationsToFileTree)
        jarTask.from(files("${rootProject.projectDir}/LICENSE"))
        tasks.maybeCreate("allShadowJars").also {
            it.dependsOn(jarTask)
            it.group = "shadow"
        }
        log(
            "configure task ${jarTask.path} for assembling fat jars",
            platform?.let { " on $it" },
            entryPoint.orNull?.let { ", with entry point $it" },
            excludedPlatforms.takeIf { it.isNotEmpty() }?.let { ", excluding dependencies for platforms $it" }
                ?: ", including all dependencies",
        )
    }
}

private fun Project.configureJarFromFileCollection(
    jarTask: ShadowJar,
    fileCollection: FileCollection,
    shouldBeIncluded: (File) -> Boolean,
    toFileTree: (Set<FileSystemLocation>) -> FileTree,
) = fileCollection.filter { it.exists() }.filter(shouldBeIncluded).elements.map(toFileTree).let { jarTask.from(it) }

private fun Project.configureJarForJvmProject(
    jarTask: ShadowJar,
    shouldBeIncluded: (File) -> Boolean,
    toFileTree: (Set<FileSystemLocation>) -> FileTree,
) {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        project.extensions.configure<SourceSetContainer>("sourceSets") { sourceSets ->
            sourceSets.getByName("main") {
                configureJarFromFileCollection(jarTask, it.runtimeClasspath, shouldBeIncluded, toFileTree)
            }
        }
        jarTask.dependsOn("classes")
    }
}

private fun Project.configureJarForMpProject(
    jarTask: ShadowJar,
    shouldBeIncluded: (File) -> Boolean,
    toFileTree: (Set<FileSystemLocation>) -> FileTree,
) {
    plugins.withId("org.jetbrains.kotlin.multiplatform") { _ ->
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            it.jvm().compilations.getByName("main").run {
                for (collection in listOf(output.allOutputs, runtimeDependencyFiles)) {
                    configureJarFromFileCollection(jarTask, collection, shouldBeIncluded, toFileTree)
                }
            }
        }
        jarTask.dependsOn("jvmMainClasses")
    }
}

private fun Project.configureJarForProject(
    jarTask: ShadowJar,
    shouldBeIncluded: (File) -> Boolean,
    toFileTree: (Set<FileSystemLocation>) -> FileTree,
) {
    configureJarForMpProject(jarTask, shouldBeIncluded, toFileTree)
    configureJarForJvmProject(jarTask, shouldBeIncluded, toFileTree)
}
