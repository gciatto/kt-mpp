package io.github.gciatto.kt.mpp.helpers

import io.github.gciatto.kt.mpp.kotlin.JsBinaryType
import io.github.gciatto.kt.mpp.publishing.Developer
import io.github.gciatto.kt.mpp.publishing.Developer.Companion.getAllDevs
import io.github.gciatto.kt.mpp.utils.getVersionFromCatalog
import io.github.gciatto.kt.mpp.utils.jsPackageName
import io.github.gciatto.kt.mpp.utils.log
import io.github.gciatto.kt.mpp.utils.toURL
import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import java.net.URL
import java.util.Locale
import kotlin.reflect.KProperty0

@Suppress("LeakingThis")
internal open class MultiPlatformHelperExtensionImpl(
    private val project: Project,
) : MultiPlatformHelperExtension {
    companion object {
        private val DEFAULT_KOTLIN_VERSION = KotlinVersion.CURRENT.toString()
        private val DEFAULT_JVM_VERSION = JavaVersion.current().toString()
        private const val DEFAULT_NODE_VERSION = "latest"
    }

    private val objects: ObjectFactory
        get() = project.objects

    private inline fun <reified T : Any> propertyWithConvention(defaultValue: T?) =
        objects.property(T::class.java).let {
            if (defaultValue != null) {
                it.convention(defaultValue)
            } else {
                it
            }
        }

    private inline fun <reified T : Any> propertyWithLazyConvention(crossinline defaultValue: () -> T?) =
        objects.property(T::class.java).convention(project.provider { defaultValue() })

    private fun booleanPropertyWithConvention(defaultValue: Boolean = false) = propertyWithConvention(defaultValue)

    private fun propertyWithConvention(
        defaultValue: String? = null,
        ignoreBlank: Boolean = true,
    ) = if (ignoreBlank) {
        propertyWithConvention<String>(defaultValue?.takeIf(String::isNotBlank))
    } else {
        propertyWithConvention<String>(defaultValue)
    }

    private fun urlPropertyWithConvention(defaultValue: URL? = null) = propertyWithConvention<URL>(defaultValue)

    private fun filePropertyWithConvention(vararg files: File): RegularFileProperty =
        objects.fileProperty().let { property ->
            files.firstOrNull { it.exists() && it.isFile }?.let { property.convention { it } } ?: property
        }

    override val disableJavadocTask: Property<Boolean> = booleanPropertyWithConvention(true)

    override val allWarningsAsErrors: Property<Boolean> = booleanPropertyWithConvention(false)

    override val issuesEmail: Property<String> = propertyWithConvention()

    override val issuesUrl: Property<URL> = urlPropertyWithConvention()

    override val ktTargetJsDisable: Property<Boolean> = booleanPropertyWithConvention(false)

    override val ktTargetJvmDisable: Property<Boolean> = booleanPropertyWithConvention(false)

    override val repoOwner: Property<String> = propertyWithConvention()

    override val mavenCentralPassword: Property<String> = propertyWithConvention()

    override val mavenCentralUsername: Property<String> = propertyWithConvention()

    override val otherMavenRepo: Property<URL> = urlPropertyWithConvention()

    override val otherMavenPassword: Property<String> = propertyWithConvention()

    override val otherMavenUsername: Property<String> = propertyWithConvention()

    override val mochaTimeout: Property<String> = propertyWithConvention("10m")

    override val npmDryRun: Property<Boolean> = booleanPropertyWithConvention(false)

    override val npmOrganization: Property<String> = propertyWithConvention()

    override val npmRepo: Property<URL> = urlPropertyWithConvention()

    override val npmToken: Property<String> = propertyWithConvention()

    override val projectDescription: Property<String> =
        propertyWithLazyConvention {
            project.description
        }

    override val projectHomepage: Property<URL> = urlPropertyWithConvention()

    override val projectLicense: Property<String> = propertyWithConvention()

    override val projectLicenseUrl: Property<URL> = urlPropertyWithConvention()

    override val projectLongName: Property<String> = propertyWithConvention()

    override val scmConnection: Property<String> = propertyWithConvention()

    override val scmUrl: Property<URL> = urlPropertyWithConvention()

    override val signingKey: Property<String> = propertyWithConvention()

    override val signingPassword: Property<String> = propertyWithConvention()

    override val useKotlinBom: Property<Boolean> = booleanPropertyWithConvention()

    override val versionsFromCatalog: Property<String> = propertyWithConvention("libs")

    private fun getVersionFromCatalog(name: String): Provider<String> =
        versionsFromCatalog.flatMap { catalog ->
            project.provider {
                project.getVersionFromCatalog(name, catalog)?.requiredVersion
            }
        }

    private fun versionProvider(
        name: String,
        defaultValue: String? = null,
    ): Provider<String> =
        getVersionFromCatalog(name).let {
            if (defaultValue != null) {
                it.orElse(defaultValue)
            } else {
                it
            }
        }

    override val nodeVersion: Property<String> = propertyWithConvention(DEFAULT_NODE_VERSION)

    override val jvmVersion: Property<String> = propertyWithConvention(DEFAULT_JVM_VERSION)

    override val kotlinVersion: Property<String> = propertyWithConvention(DEFAULT_KOTLIN_VERSION)

    // override val docStyle: Property<DocStyle> = propertyWithConvention(DocStyle.HTML)

    override val developers: DomainObjectCollection<Developer> = project.objects.domainObjectSet(Developer::class.java)

    override val ktCompilerArgs: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val ktCompilerArgsJs: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val ktCompilerArgsJvm: DomainObjectSet<String> = project.objects.domainObjectSet(String::class.java)

    override val jsPackageName: Property<String> =
        propertyWithLazyConvention {
            project.jsPackageName
        }

    override val bugFinderConfigPath =
        filePropertyWithConvention(
            project.file(".detekt.yml"),
            project.rootProject.project.file(".detekt.yml"),
        )

    override val bugFinderConfig: FileCollection
        get() =
            project.objects.fileCollection().also { collection ->
                bugFinderConfigPath.orNull?.takeIf { it.asFile.exists() }?.let {
                    collection.from(it)
                }
            }

    override val jsBinaryType: Property<JsBinaryType> = propertyWithConvention(JsBinaryType.LIBRARY)

    override val fatJarPlatforms: DomainObjectSet<String> = objects.domainObjectSet(String::class.java)

    override val fatJarClassifier: Property<String> = propertyWithConvention("redist")

    @Suppress("UNCHECKED_CAST")
    override val fatJarPlatformInclusions: DomainObjectSet<Pair<String, String>> =
        project.objects.domainObjectSet(Pair::class.java) as DomainObjectSet<Pair<String, String>>

    override val fatJarEntryPoint: Property<String> = propertyWithConvention()

    override fun populateArgumentsFromProperties() {
        for (property in listOf(::ktCompilerArgs, ::ktCompilerArgsJvm, ::ktCompilerArgsJs)) {
            project.findProperty(property.name)?.let { value ->
                property.get().addAll(value.toString().separateBy(';'))
            }
        }
    }

    override fun populateDevelopersFromProperties() {
        developers.addAll(project.getAllDevs())
    }

    override fun populateFatJarPlatformsFromNames() {
        fatJarPlatforms.addAll(
            project
                .findProperty(::fatJarPlatforms.name)
                ?.toString()
                ?.separateBy(';')
                .orEmpty(),
        )
    }

    override fun populateFatJarPlatformIncludesFromProperties() {
        fun parse(input: String?) =
            input
                ?.separateBy(';')
                ?.flatMap { pair ->
                    val (key, values) = pair.separateBy(':').let { it[0] to it[1] }
                    values.separateBy(',').map { key to it }
                }.orEmpty()
        fatJarPlatformInclusions.addAll(
            parse(project.findProperty(::fatJarPlatformInclusions.name)?.toString()),
        )
    }

    private fun String.separateBy(separator: Char) = split(separator).filter { it.isNotBlank() }.map { it.trim() }

    context(x: KProperty0<*>)
    private fun <T> T?.returnLogging(): T? =
        this.also {
            if (it == null) {
                project.log("gradle property '${x.name}' is unset or blank")
            } else {
                project.log("infer ${x.name} from homonymous gradle property of value '$it'")
            }
        }

    private fun <T : Any> KProperty0<Property<T>>.populateFromProperty(converter: (String) -> T?): T? =
        project
            .findProperty(name)
            ?.let { converter(it.toString()) }
            ?.let {
                get().set(it)
                it
            }.returnLogging()

    private fun KProperty0<RegularFileProperty>.populateFromProperty(): File? =
        project
            .findProperty(name)
            ?.let { project.file(it.toString()) }
            ?.let {
                get().set(it)
                it
            }.returnLogging()

    private fun KProperty0<Property<URL>>.populateFromProperty() =
        populateFromProperty { runCatching { it.toURL() }.takeIf(Result<*>::isSuccess)?.getOrNull() }

    private fun KProperty0<Property<String>>.populateFromProperty(ignoreBlank: Boolean = true) =
        populateFromProperty {
            if (ignoreBlank) {
                it.takeIf(String::isNotBlank)
            } else {
                it
            }
        }

    private fun KProperty0<Property<Boolean>>.populateFromProperty() = populateFromProperty { it.toBooleanStrict() }

    override fun initializeVersionsRelatedProperties(
        jvm: Boolean,
        node: Boolean,
    ) {
        ::versionsFromCatalog.populateFromProperty()
        ::kotlinVersion.populateFromProperty()
            ?: kotlinVersion.set(versionProvider("kotlin", DEFAULT_KOTLIN_VERSION))
        if (jvm) {
            ::jvmVersion.populateFromProperty()
                ?: jvmVersion.set(versionProvider("jvm", DEFAULT_KOTLIN_VERSION))
        }
        if (node) {
            ::nodeVersion.populateFromProperty()
                ?: nodeVersion.set(versionProvider("node", DEFAULT_NODE_VERSION))
        }
    }

    override fun initializeKotlinRelatedProperties() {
        ::allWarningsAsErrors.populateFromProperty()
        ::ktTargetJsDisable.populateFromProperty()
        ::ktTargetJvmDisable.populateFromProperty()
        ::useKotlinBom.populateFromProperty()
        populateArgumentsFromProperties()
    }

    override fun initializeJsRelatedProperties() {
        ::mochaTimeout.populateFromProperty()
        ::jsBinaryType.populateFromProperty { str ->
            str.takeIf(String::isNotBlank)?.let { JsBinaryType.valueOf(it.uppercase(Locale.getDefault())) }
        }
    }

    override fun initializeJvmRelatedProperties() {
        ::disableJavadocTask.populateFromProperty()
    }

    override fun initializeFatJarRelatedProperties() {
        ::fatJarEntryPoint.populateFromProperty()
        ::fatJarClassifier.populateFromProperty()
        populateFatJarPlatformsFromNames()
        populateFatJarPlatformIncludesFromProperties()
    }

    override fun initializeBugFinderRelatedProperties() {
        ::bugFinderConfigPath.populateFromProperty()
    }

    override fun initializeMavenRelatedProperties() {
        ::issuesEmail.populateFromProperty()
        ::issuesUrl.populateFromProperty()
        ::repoOwner.populateFromProperty()
        ::mavenCentralPassword.populateFromProperty()
        ::mavenCentralUsername.populateFromProperty()
        ::otherMavenRepo.populateFromProperty()
        ::otherMavenPassword.populateFromProperty()
        ::otherMavenUsername.populateFromProperty()
        ::projectDescription.populateFromProperty()
        ::projectHomepage.populateFromProperty()
        ::projectLicense.populateFromProperty()
        ::projectLicenseUrl.populateFromProperty()
        ::projectLongName.populateFromProperty()
        ::scmConnection.populateFromProperty()
        ::scmUrl.populateFromProperty()
        ::signingKey.populateFromProperty()
        ::signingPassword.populateFromProperty()
        // ::docStyle.populateFromProperty { str ->
        //    str.takeIf(String::isNotBlank)?.let { DocStyle.valueOf(it.uppercase(Locale.getDefault())) }
        // }
        populateDevelopersFromProperties()
    }

    override fun initializeNpmRelatedProperties() {
        ::jsPackageName.populateFromProperty()
        ::npmDryRun.populateFromProperty()
        ::npmOrganization.populateFromProperty()
        ::npmRepo.populateFromProperty()
        ::npmToken.populateFromProperty()
        populateDevelopersFromProperties()
    }
}
