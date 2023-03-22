package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class NativeOnlyPlugin : AbstractKotlinProjectPlugin("native") {
    override fun Project.applyThisPlugin() {
        apply(plugin = kotlinPlugin())
        log("apply ${kotlinPlugin()} plugin")
        apply(plugin = "native")
        log("apply native plugin")
        configureKotlinVersionFromCatalogIfPossible()
        configure(KotlinMultiplatformExtension::class) {
            sourceSets.create("nativeMain")
            sourceSets.create("nativeTest")
            sourceSets.getByName("nativeMain").dependsOn(sourceSets["commonMain"])
            sourceSets.getByName("nativeTest").dependsOn(sourceSets["commonTest"])

            val nativeSetup: KotlinNativeTarget.() -> Unit = {
                compilations["main"].defaultSourceSet.dependsOn(sourceSets["nativeMain"])
                compilations["test"].defaultSourceSet.dependsOn(sourceSets["nativeTest"])
                binaries {
                    sharedLib()
                    staticLib()
                }
            }
            // TODO: enable selectively the targets based on properties
            linuxX64(nativeSetup)
            linuxArm64(nativeSetup)

            mingwX64(nativeSetup)

            macosX64(nativeSetup)
            macosArm64(nativeSetup)
            ios(nativeSetup)
            watchos(nativeSetup)
            tvos(nativeSetup)
        }
        addTaskAliases()
    }

    override fun PropertiesHelperExtension.declareProperties() {
        addProperty(allWarningsAsErrors)
        addProperty(ktCompilerArgs)
        addProperty(ktCompilerArgsJvm)
        addProperty(mochaTimeout)
        addProperty(versionsFromCatalog)
    }
}
