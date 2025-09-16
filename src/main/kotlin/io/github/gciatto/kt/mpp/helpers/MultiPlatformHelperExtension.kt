package io.github.gciatto.kt.mpp.helpers

import io.github.gciatto.kt.mpp.kotlin.JsBinaryType
import io.github.gciatto.kt.mpp.publishing.Developer
import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import java.net.URL

interface MultiPlatformHelperExtension {
    val disableJavadocTask: Property<Boolean>
    val allWarningsAsErrors: Property<Boolean>
    val developers: DomainObjectCollection<Developer>
    val issuesEmail: Property<String>
    val issuesUrl: Property<URL>
    val ktCompilerArgs: DomainObjectSet<String>
    val ktCompilerArgsJs: DomainObjectSet<String>
    val ktCompilerArgsJvm: DomainObjectSet<String>
    val ktTargetJsDisable: Property<Boolean>
    val ktTargetJvmDisable: Property<Boolean>
    val repoOwner: Property<String>
    val mavenCentralPassword: Property<String>
    val mavenCentralUsername: Property<String>
    val otherMavenRepo: Property<URL>
    val otherMavenPassword: Property<String>
    val otherMavenUsername: Property<String>
    val mochaTimeout: Property<String>
    val npmDryRun: Property<Boolean>
    val npmOrganization: Property<String>
    val npmRepo: Property<URL>
    val npmToken: Property<String>
    val projectDescription: Property<String>
    val projectHomepage: Property<URL>
    val projectLicense: Property<String>
    val projectLicenseUrl: Property<URL>
    val projectLongName: Property<String>
    val scmConnection: Property<String>
    val scmUrl: Property<URL>
    val signingKey: Property<String>
    val signingPassword: Property<String>
    val useKotlinBom: Property<Boolean>
    val versionsFromCatalog: Property<String>
    val nodeVersion: Property<String>
    val kotlinVersion: Property<String>
    val jvmVersion: Property<String>

    // val docStyle: Property<DocStyle>
    val jsPackageName: Property<String>
    val bugFinderConfigPath: RegularFileProperty
    val bugFinderConfig: FileCollection
    val jsBinaryType: Property<JsBinaryType>
    val fatJarPlatforms: DomainObjectSet<String>
    val fatJarClassifier: Property<String>
    val fatJarPlatformInclusions: DomainObjectSet<Pair<String, String>>
    val fatJarEntryPoint: Property<String>

    fun fatJarPlatformInclude(
        platform: String,
        vararg includes: String,
    ) = includes.forEach { fatJarPlatformInclusions.add(platform to it) }

    fun populateArgumentsFromProperties()

    fun populateDevelopersFromProperties()

    fun populateFatJarPlatformsFromNames()

    fun populateFatJarPlatformIncludesFromProperties()

    fun initializeVersionsRelatedProperties(
        jvm: Boolean = true,
        node: Boolean = true,
    )

    fun initializeKotlinRelatedProperties()

    fun initializeJsRelatedProperties()

    fun initializeJvmRelatedProperties()

    fun initializeFatJarRelatedProperties()

    fun initializeBugFinderRelatedProperties()

    fun initializeMavenRelatedProperties()

    fun initializeNpmRelatedProperties()
}
