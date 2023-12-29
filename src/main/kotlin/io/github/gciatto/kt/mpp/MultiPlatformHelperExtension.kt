package io.github.gciatto.kt.mpp

import io.github.gciatto.kt.mpp.Developer.Companion.getAllDevs
import org.danilopianini.gradle.mavencentral.DocStyle
import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import java.net.URL
import java.util.Locale
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

interface MultiPlatformHelperExtension {
    val disableJavadocTask: Property<Boolean>
    val allWarningsAsErrors: Property<Boolean>
    val developers: DomainObjectCollection<Developer>
    val issuesEmail: Property<String>
    val issuesUrl: Property<URL>
    val ktCompilerArguments: DomainObjectSet<String>
    val ktCompilerArgumentsJs: DomainObjectSet<String>
    val ktCompilerArgumentsJvm: DomainObjectSet<String>
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
    val docStyle: Property<DocStyle>
    val jsPackageName: Property<String>
    val bugFinderConfigPath: Property<File>
    val bugFinderConfig: FileCollection
    val jsBinaryType: Property<JsBinaryType>
    val fatJarPlatforms: DomainObjectSet<String>
    val fatJarClassifier: Property<String>
    val fatJarPlatformInclusions: DomainObjectSet<Pair<String, String>>
    val fatJarEntryPoint: Property<String>
    fun fatJarPlatformInclude(platform: String, vararg includes: String) =
        includes.forEach { fatJarPlatformInclusions.add(platform to it) }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> MultiPlatformHelperExtension.getProvider(property: KProperty<*>): Provider<T> =
    this::class.memberProperties.find { it.name == property.name }
        .let { it as KProperty1<MultiPlatformHelperExtension, Provider<T>> }
        .get(this)

@Suppress("LeakingThis")
internal open class MultiPlatformHelperExtensionImpl(project: Project) : MultiPlatformHelperExtension {

    private val project: Project = project.rootProject

    private class PropertyWithConvention<T>(
        private val klass: Class<T>,
        private val defaultValue: Provider<T>,
        private val converter: (String) -> T?,
    ) {
        operator fun getValue(self: MultiPlatformHelperExtensionImpl, property: KProperty<*>): Property<T> {
            var lazyValue = self.project.provider {
                if (property.name in self.project.properties) {
                    converter(self.project.property(property.name).toString())
                } else {
                    null
                }
            }
            if (self.project.let { it != it.rootProject }) {
                lazyValue = lazyValue.orElse(self.project.rootProject.multiPlatformHelper.getProvider(property))
            }
            lazyValue = lazyValue.orElse(defaultValue)
            return self.project.objects.property(klass).convention(lazyValue)
        }
    }

    private inline fun <reified T : Any> propertyWithConvention(
        defaultValue: Provider<T>,
        noinline converter: (String) -> T?,
    ) = PropertyWithConvention(T::class.java, defaultValue, converter)

    private inline fun <reified T : Any> propertyWithConvention(
        defaultValue: T? = null,
        noinline converter: (String) -> T?,
    ) = propertyWithConvention(project.provider<T> { defaultValue }, converter)

    private fun booleanPropertyWithConvention(defaultValue: Boolean = false) =
        propertyWithConvention(defaultValue) { it.toBooleanStrict() }

    private fun propertyWithConvention(defaultValue: String? = null, ignoreBlank: Boolean = true) =
        propertyWithConvention(defaultValue) { if (it.isBlank() && ignoreBlank) null else it }

    private fun urlPropertyWithConvention(defaultValue: URL? = null) =
        propertyWithConvention<URL>(defaultValue) {
            if (it.isBlank()) {
                null
            } else {
                val url = runCatching { it.toURL() }
                if (url.isFailure) {
                    project.log("invalid URL: $it", LogLevel.WARN)
                }
                url.getOrNull()
            }
        }

    private fun filePropertyWithConvention(defaultValue: File? = null) =
        propertyWithConvention<File>(defaultValue) {
            if (it.isBlank()) {
                null
            } else {
                val file = runCatching { File(it) }
                if (file.isFailure) {
                    project.log("invalid file: $it", LogLevel.WARN)
                } else if (file.isSuccess && !file.getOrThrow().exists()) {
                    project.log("file does not exist: $it", LogLevel.WARN)
                }
                file.getOrNull()
            }
        }

    override val disableJavadocTask: Property<Boolean> by booleanPropertyWithConvention(true)

    override val allWarningsAsErrors: Property<Boolean> by booleanPropertyWithConvention(false)

    override val issuesEmail: Property<String> by propertyWithConvention()

    override val issuesUrl: Property<URL> by urlPropertyWithConvention()

    override val ktTargetJsDisable: Property<Boolean> by booleanPropertyWithConvention(false)

    override val ktTargetJvmDisable: Property<Boolean> by booleanPropertyWithConvention(false)

    override val repoOwner: Property<String> by propertyWithConvention()

    override val mavenCentralPassword: Property<String> by propertyWithConvention()

    override val mavenCentralUsername: Property<String> by propertyWithConvention()

    override val otherMavenRepo: Property<URL> by urlPropertyWithConvention()

    override val otherMavenPassword: Property<String> by propertyWithConvention()

    override val otherMavenUsername: Property<String> by propertyWithConvention()

    override val mochaTimeout: Property<String> by propertyWithConvention("10m")

    override val npmDryRun: Property<Boolean> by booleanPropertyWithConvention(false)

    override val npmOrganization: Property<String> by propertyWithConvention()

    override val npmRepo: Property<URL> by urlPropertyWithConvention()

    override val npmToken: Property<String> by propertyWithConvention()

    override val projectDescription: Property<String> by propertyWithConvention(
        project.provider<String> { project.description },
    ) { it }

    override val projectHomepage: Property<URL> by urlPropertyWithConvention()

    override val projectLicense: Property<String> by propertyWithConvention()

    override val projectLicenseUrl: Property<URL> by urlPropertyWithConvention()

    override val projectLongName: Property<String> by propertyWithConvention()

    override val scmConnection: Property<String> by propertyWithConvention()

    override val scmUrl: Property<URL> by urlPropertyWithConvention()

    override val signingKey: Property<String> by propertyWithConvention()

    override val signingPassword: Property<String> by propertyWithConvention()

    override val useKotlinBom: Property<Boolean> by booleanPropertyWithConvention()

    override val versionsFromCatalog: Property<String> by propertyWithConvention("libs")

    private fun getVersionFromCatalog(name: String): Provider<String> =
        versionsFromCatalog.flatMap { catalog ->
            project.provider {
                project.getVersionFromCatalog(name, catalog)?.requiredVersion
            }
        }

    private fun versionProperty(name: String, defaultValue: String? = null) = propertyWithConvention(
        defaultValue = getVersionFromCatalog(name).orElse("latest").let {
            if (defaultValue != null) {
                it.orElse(defaultValue)
            } else {
                it
            }
        },
        converter = { it.takeIf(String::isNotBlank) },
    )

    override val nodeVersion: Property<String> by versionProperty("node", "latest")

    override val jvmVersion: Property<String> by versionProperty("jvm", JavaVersion.current().toString())

    override val kotlinVersion: Property<String> by versionProperty("kotlin")

    override val docStyle: Property<DocStyle> by propertyWithConvention(DocStyle.HTML) {
        DocStyle.valueOf(it.uppercase(Locale.getDefault()))
    }

    override val developers: DomainObjectCollection<Developer> = project.objects.domainObjectSet(Developer::class.java)

    private val ktCompilerArgs: Property<String> by propertyWithConvention("")

    private val ktCompilerArgsJs: Property<String> by propertyWithConvention("")

    private val ktCompilerArgsJvm: Property<String> by propertyWithConvention("")

    override val ktCompilerArguments: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val ktCompilerArgumentsJs: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val ktCompilerArgumentsJvm: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val jsPackageName: Property<String> by propertyWithConvention(
        defaultValue = project.provider { project.jsPackageName },
        converter = { it },
    )

    override val bugFinderConfigPath: Property<File> by filePropertyWithConvention(
        project.rootProject.file(".detekt.yml"),
    )

    override val bugFinderConfig: FileCollection
        get() = project.objects.fileCollection().also { collection ->
            bugFinderConfigPath.orNull?.takeIf { it.exists() }?.let {
                collection.from(it)
            }
        }

    override val jsBinaryType: Property<JsBinaryType> by propertyWithConvention(JsBinaryType.LIBRARY) {
        JsBinaryType.valueOf(it.uppercase(Locale.getDefault()))
    }

    internal val fatJarPlatformNames: Property<String> by propertyWithConvention("", ignoreBlank = false)

    override val fatJarPlatforms: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val fatJarClassifier: Property<String> by propertyWithConvention("redist")

    private val fatJarPlatformInclude: Property<String> by propertyWithConvention("")

    @Suppress("UNCHECKED_CAST")
    override val fatJarPlatformInclusions: DomainObjectSet<Pair<String, String>> =
        project.objects.domainObjectSet(Pair::class.java) as DomainObjectSet<Pair<String, String>>

    override val fatJarEntryPoint: Property<String> by propertyWithConvention()

    private fun populateArgumentsFromArgs() {
        for ((string, collection) in listOf(
            ktCompilerArgs to ktCompilerArguments,
            ktCompilerArgsJvm to ktCompilerArgumentsJvm,
            ktCompilerArgsJs to ktCompilerArgumentsJs,
        )) {
            collection.addAllLater(string.separateBy(';'))
        }
    }

    private fun populateDevelopersFromProperties() {
        developers.addAllLater(project.provider { project.getAllDevs() })
    }

    internal fun populateFatJarPlatformsFromNames() {
        fatJarPlatforms.addAllLater(fatJarPlatformNames.separateBy(';'))
    }

    private fun populateFatJarPlatformIncludesFromProperties() {
        fun parse(input: String?) = input?.separateBy(';')
            ?.flatMap { pair ->
                val (key, values) = pair.separateBy(':').let { it[0] to it[1] }
                values.separateBy(',').map { key to it }
            } ?: emptyList()
        fatJarPlatformInclusions.addAllLater(fatJarPlatformInclude.map { parse(it) })
    }

    private fun String.separateBy(separator: Char) =
        split(separator).filter { it.isNotBlank() }.map { it.trim() }

    private fun Provider<String>.separateBy(separator: Char) =
        map { it.toString().separateBy(separator) }

    init {
        populateArgumentsFromArgs()
        populateDevelopersFromProperties()
        populateFatJarPlatformsFromNames()
        populateFatJarPlatformIncludesFromProperties()
    }
}
