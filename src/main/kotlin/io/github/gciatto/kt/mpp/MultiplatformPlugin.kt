package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class MultiplatformPlugin : AbstractKotlinProjectPlugin("multiplatform") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        configureKotlinVersionFromCatalogIfPossible()
        configureJvmVersionFromCatalogIfPossible()
        configureNodeVersionFromCatalogIfPossible()
        val ktTargetJvmDisable = getBooleanProperty("ktTargetJvmDisable")
        val ktTargetJsDisable = getBooleanProperty("ktTargetJsDisable")
        val ktTargetNativeDisable = getBooleanProperty("ktTargetNativeDisable")
        configure(KotlinMultiplatformExtension::class) {
            if (ktTargetJvmDisable) {
                log("disable JVM target", LogLevel.WARN)
            } else {
                jvm { configureJvm() }
            }
            if (ktTargetJsDisable) {
                log("disable JS target", LogLevel.WARN)
            } else {
                js { configureJs() }
            }
            if (ktTargetNativeDisable) {
                log("disable Native target", LogLevel.WARN)
            } else {
                configureNative()
            }
            dependenciesFor("commonMain") {
                addMainDependencies(project, "common", skipBom = false)
            }
            dependenciesFor("commonTest") {
                addTestDependencies(project, "common", skipAnnotations = false)
            }
            targets.all { target ->
                target.compilations.all { compilation ->
                    compilation.kotlinOptions {
                        configureKotlinOptions(target.targetCompilationId(compilation))
                    }
                }
            }
        }
    }

    private fun KotlinMultiplatformExtension.dependenciesFor(
        sourceSet: String,
        action: KotlinDependencyHandler.() -> Unit,
    ) = sourceSets.getByName(sourceSet).dependencies(action)

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJvmTarget.configureJvm() {
        withJava()
        log("configure Kotlin JVM target to accept Java sources")
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJvmKotlinOptions(targetCompilationId(compilation))
            }
        }
        dependenciesFor("jvmMain") {
            addMainDependencies(project, "jdk8", skipBom = true)
        }
        dependenciesFor("jvmTest") {
            addTestDependencies(project, "junit", skipAnnotations = true)
        }
    }

    context(Project, KotlinMultiplatformExtension)
    private fun KotlinJsTargetDsl.configureJs() {
        compilations.all { compilation ->
            compilation.kotlinOptions {
                configureJsKotlinOptions(targetCompilationId(compilation))
            }
        }
        configureNodeJs()
        dependenciesFor("jsMain") {
            addMainDependencies(project, "js", skipBom = true)
        }
        dependenciesFor("jsTest") {
            addTestDependencies(project, "js", skipAnnotations = true)
        }
    }

    context(Project)
    private fun KotlinMultiplatformExtension.configureNative() {
        val disableLinuxX64 = getBooleanProperty("linuxX64Disable")
        val disableLinuxArm64 = getBooleanProperty("linuxArm64Disable")
        val disableMingwX64 = getBooleanProperty("mingwX64Disable")
        val disableMacosX64 = getBooleanProperty("macosX64Disable")
        val disableMacosArm64 = getBooleanProperty("macosArm64Disable")
        val disableIos = getBooleanProperty("iosDisable")
        val disableWatchos = getBooleanProperty("watchosDisable")
        val disableTvos = getBooleanProperty("tvosDisable")
        val nativeCrossCompilationEnable = getBooleanProperty("nativeCrossCompilationEnable")
        val nativeStaticLib = getBooleanProperty("nativeStaticLib")
        val nativeSharedLib = getBooleanProperty("nativeSharedLib")
        val nativeExecutable = getBooleanProperty("nativeExecutable")

        sourceSets.create("nativeMain").dependsOn(sourceSets["commonMain"])
        sourceSets.create("nativeTest").dependsOn(sourceSets["commonTest"])

        val nativeSetup: KotlinNativeTarget.() -> Unit = {
            compilations["main"].defaultSourceSet.dependsOn(sourceSets["nativeMain"])
            compilations["test"].defaultSourceSet.dependsOn(sourceSets["nativeTest"])
            binaries {
                if (nativeStaticLib) { staticLib() }
                if (nativeSharedLib) { sharedLib() }
                if (nativeExecutable) { executable() }
            }
        }

        if (!disableLinuxX64) linuxX64(nativeSetup)
        if (!disableLinuxArm64) linuxArm64(nativeSetup)

        if (!disableMingwX64) mingwX64(nativeSetup)

        if (!disableMacosX64) macosX64(nativeSetup)
        if (!disableMacosArm64) macosArm64(nativeSetup)
        if (!disableIos) ios(nativeSetup)
        if (!disableWatchos) watchos(nativeSetup)
        if (!disableTvos) tvos(nativeSetup)

        if (!nativeCrossCompilationEnable) {
            disableCrossCompilation()
        } else {
            log("Cross compilation for native target enabled", LogLevel.WARN)
        }
    }

    context(Project)
    private fun KotlinMultiplatformExtension.disableCrossCompilation() {
        // Disable cross compilation
        val os = OperatingSystem.current()
        val excludeTargets = when {
            os.isLinux -> targets.filterNot { "linux" in it.name }
            os.isWindows -> targets.filterNot { "mingw" in it.name }
            os.isMacOsX -> targets.filter { "linux" in it.name || "mingw" in it.name }
            else -> emptyList()
        }.mapNotNull { it as? KotlinNativeTarget }

        configure(excludeTargets) { target ->
            target.compilations.configureEach { nativeCompilation ->
                nativeCompilation.cinterops.configureEach { tasks[it.interopProcessingTaskName].enabled = false }
                nativeCompilation.compileTaskProvider.get().enabled = false
                tasks[nativeCompilation.processResourcesTaskName].enabled = false
            }
            target.binaries.configureEach { it.linkTask.enabled = false }

            target.mavenPublication {
                tasks.withType<AbstractPublishToMaven>()
                    .configureEach { maven -> maven.onlyIf { maven.publication != this@mavenPublication } }
                tasks.withType<GenerateModuleMetadata>().configureEach { metadata ->
                    metadata.onlyIf { metadata.publication.get() != this@mavenPublication }
                }
            }
        }
    }

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(allWarningsAsErrors)
        addProperty(ktCompilerArgs)
        addProperty(ktCompilerArgsJvm)
        addProperty(ktCompilerArgsJs)
        addProperty(mochaTimeout)
        addProperty(ktTargetJvmDisable)
        addProperty(ktTargetJsDisable)
        addProperty(ktTargetNativeDisable)
        addProperty(linuxX64Disable)
        addProperty(linuxArm64Disable)
        addProperty(mingwX64Disable)
        addProperty(macosX64Disable)
        addProperty(macosArm64Disable)
        addProperty(iosDisable)
        addProperty(watchOsDisable)
        addProperty(tvOsDisable)
        addProperty(nativeCrossCompilationEnable)
        addProperty(nativeStaticLib)
        addProperty(nativeSharedLib)
        addProperty(nativeExecutable)
        addProperty(versionsFromCatalog)
        addProperty(nodeVersion)
    }
}
