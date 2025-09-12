@file:Suppress("OPT_IN_USAGE")

import de.aaschmid.gradle.plugins.cpd.Cpd
import io.gitlab.arturbosch.detekt.Detekt
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.jacoco.testkit)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

/*
 * Project information
 */
group = "io.github.gciatto"
description = "Kotlin multi-platform and multi-project configurations plugin for Gradle"

inner class ProjectInfo {
    val longName = "Advanced Kotlin multi-platform plugin for Gradle Plugins"
    val website = "https://github.com/gciatto/$name"
    val vcsUrl = "$website.git"
    val scm = "scm:git:$website.git"
    val pluginImplementationClass = "$group.kt.mpp"
    val tags = listOf("kotlin", "multi-platform")
}

val info = ProjectInfo()

gitSemVer {
    buildMetadataSeparator.set("-")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val jvmVersion =
    libs.versions.jvm
        .map { JavaVersion.toVersion(it) }
        .getOrElse(JavaVersion.VERSION_11)

java {
    targetCompatibility = jvmVersion
    sourceCompatibility = jvmVersion
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    api(kotlin("stdlib-jdk8"))
    implementation(libs.kotlin.bom)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.dokka)
    implementation(libs.ktlint)
    implementation(libs.detekt)
    implementation(libs.publishOnCentral)
    implementation(libs.shadowJar)
    implementation(libs.npmPublish)
    testImplementation(gradleTestKit())
    testImplementation(libs.konf.yaml)
    testImplementation(libs.classgraph)
    testImplementation(libs.bundles.kotlin.testing)
}

// Enforce Kotlin version coherence
configurations.matching { "detekt" !in it.name }.all {
    val configuration = this
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
            useVersion(KOTLIN_VERSION)
            val artifact = "${requested.group}:${requested.name}"
            because("Force version $version for $artifact in configuration ${configuration.name}")
        }
    }
}

kotlin {
    target {
        compilerOptions {
            allWarningsAsErrors = true
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xcontext-parameters")
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

inline fun <reified T : Task> Project.disableTrackStateOnWindows() {
    tasks.withType<T>().configureEach {
        doNotTrackState("Windows is a mess and JaCoCo does not work correctly")
    }
}

if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    disableTrackStateOnWindows<Test>()
    disableTrackStateOnWindows<JacocoReport>()
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn(tasks.generateJacocoTestKitProperties)
    testLogging {
        showStandardStreams = true
        showCauses = true
        showStackTraces = true
        events(
            *org.gradle.api.tasks.testing.logging.TestLogEvent
                .values(),
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

detekt {
    config.from(".detekt-config.yml")
    buildUponDefaultConfig = true
}

signing {
    if (System.getenv()["CI"].equals("true", ignoreCase = true)) {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

/*
 * Publication on Maven Central and the Plugin portal
 */
publishOnCentral {
    configureMavenCentral.set(false)
    projectLongName.set(info.longName)
    projectDescription.set(description ?: TODO("Missing description"))
    projectUrl.set(info.website)
    scmConnection.set(info.scm)
    repository("https://maven.pkg.github.com/gciatto/${rootProject.name}".lowercase(), name = "github") {
        user.set("gciatto")
        password.set(System.getenv("GITHUB_TOKEN"))
    }
    publishing {
        publications {
            withType<MavenPublication> {
                pom {
                    developers {
                        developer {
                            name.set("Giovanni Ciatto")
                            email.set("giovanni.ciatto@gmail.com")
                            url.set("https://www.about.me/gciatto")
                        }
                    }
                }
            }
        }
    }
}

class PluginDescriptor(
    val name: String,
    val fullClass: String,
) {
    val simpleClass: String
        get() = fullClass.split(".").last()

    private val simpleName: String
        get() = name.split(".").last()

    private val kotlinId: String
        get() =
            simpleName
                .split("-")
                .mapIndexed { i, s -> if (i > 0) s.capitalized() else s }
                .joinToString("")

    @Suppress("NAME_SHADOWING")
    fun generateKotlinMethod(indent: Int = 4): String {
        val indent = " ".repeat(indent)
        return "${indent}val $kotlinId = PluginDescriptor(\n" +
            "${indent.repeat(2)}\"$name\",\n" +
            "${indent.repeat(2)}$fullClass::class,\n" +
            "$indent)\n"
    }
}

gradlePlugin {
    plugins {
        website.set(info.website)
        vcsUrl.set(info.vcsUrl)

        val pluginsClasses = mutableSetOf<PluginDescriptor>()

        fun innerPlugin(
            name: String,
            confName: String = name,
            descr: String,
            klass: String,
            vararg moreTags: String,
        ) = create(name) {
            id = "$group.${project.name}.$name"
            displayName = "Default $confName configuration for Kotlin multi-platform projects"
            description = "${project.description}: $descr"
            implementationClass = "${info.pluginImplementationClass}.$klass"
            tags.set(info.tags + listOf(*moreTags))
            pluginsClasses += PluginDescriptor(id, implementationClass)
        }

        innerPlugin(
            name = "bug-finder",
            descr = "bug-finder (currently, Detekt)",
            klass = "verification.BugFinderPlugin",
            moreTags = arrayOf("bug-finder", "detekt"),
        )

        innerPlugin(
            name = "documentation",
            descr = "documentation generator (currently, Dokka)",
            klass = "documentation.DocumentationPlugin",
            moreTags = arrayOf("documentation", "dokka"),
        )

        innerPlugin(
            name = "linter",
            descr = "linter (currently, KtLint)",
            klass = "verification.LinterPlugin",
            moreTags = arrayOf("linter", "ktlint"),
        )

        innerPlugin(
            name = "maven-publish",
            descr = "maven publication",
            klass = "publishing.PublishOnMavenPlugin",
            moreTags = arrayOf("maven"),
        )

        innerPlugin(
            name = "npm-publish",
            descr = "npm publication",
            klass = "publishing.PublishOnNpmPlugin",
            moreTags = arrayOf("npm"),
        )

        innerPlugin(
            name = "versions",
            descr = "version revealer",
            klass = "versioning.VersionsPlugin",
            moreTags = arrayOf("version"),
        )

        innerPlugin(
            name = "js-only",
            descr = "JS only project configuration",
            klass = "kotlin.JsOnlyPlugin",
            moreTags = arrayOf("js"),
        )

        innerPlugin(
            name = "jvm-only",
            descr = "JVM only project configuration",
            klass = "kotlin.JvmOnlyPlugin",
            moreTags = arrayOf("jvm"),
        )

        innerPlugin(
            name = "multiplatform",
            descr = "multi-platform project configuration",
            klass = "kotlin.MultiplatformPlugin",
            moreTags = arrayOf("multiplatform"),
        )

        innerPlugin(
            name = "multi-project-helper",
            descr = "multi-platform & multi-project helper plugin",
            klass = "helpers.MultiProjectHelperPlugin",
            moreTags = arrayOf(),
        )

        innerPlugin(
            name = "fat-jar",
            descr = "fat-jar creator plugin (currently: Shadow)",
            klass = "jar.FatJarPlugin",
            moreTags = arrayOf("fat-jar", "uber-jar", "redist", "shadow"),
        )

        tasks.create("generatePluginsInfo") {
            group = "build"
            val pkg = project.group.toString().replace('.', '/')
            sourceSets.main {
                val targetFile = kotlin.srcDirs.first { it.name == "kotlin" }.resolve("$pkg/kt/mpp/Plugins.kt")
                outputs.file(targetFile)
                doLast {
                    @Suppress("ktlint")
                    val text = "@file:Suppress(\"MaxLineLength\", \"ktlint\")\n\n" +
                            "package ${project.group}.kt.mpp\n\n" +
                            "object Plugins {\n" +
                            pluginsClasses.joinToString("\n") { it.generateKotlinMethod() } +
                            "}\n"
                    targetFile.writeText(text)
                }
            }
            tasks.getByName("sourcesJar").dependsOn(this)
            tasks.getByName("compileKotlin").dependsOn(this)
            tasks.getByName("detekt").dependsOn(this)
            tasks.withType(DokkaTask::class.java) { dependsOn(this@create) }
            tasks.withType(Cpd::class.java) { dependsOn(this@create) }
            tasks.withType(BaseKtLintCheckTask::class.java) { dependsOn(this@create) }
        }
    }
}

tasks
    .withType(Detekt::class.java)
    .matching { task -> task.name.let { it.endsWith("Main") || it.endsWith("Test") } }
    .all {
        val detektTask = this
        tasks.check.configure { dependsOn(detektTask) }
    }

tasks.create("uploadAllPluginMarkersToMavenCentralNexus") {
    group = "publishing"
    description = "Quick way to call tasks upload*PluginMarkerMavenToMavenCentralNexus altogether"
    tasks.withType<PublishToMavenRepository> {
        if (name.startsWith("upload") && name.endsWith("PluginMarkerMavenToMavenCentralNexus")) {
            this@create.dependsOn(this)
        }
    }
}

tasks.create("uploadToMavenCentralNexus") {
    group = "publishing"
    description = "Quick upload relevant publication to Nexus, altogether"
    dependsOn(
        "uploadAllPluginMarkersToMavenCentralNexus",
        "uploadKotlinOSSRHToMavenCentralNexus",
        "uploadPluginMavenToMavenCentralNexus",
    )
}

fun testDirectories(): Set<File> =
    buildSet {
        sourceSets.test {
            resources.srcDirs.forEach { testResourcesDir ->
                fileTree(testResourcesDir) { include("**/test.yaml") }.asFileTree.visit {
                    if (!isDirectory) {
                        add(file.parentFile)
                    }
                }
            }
        }
    }

for (testDir in testDirectories()) {
    tasks.create<Copy>("copyLibsTo${testDir.name.capitalized()}") {
        group = "verification"
        description = "Copies the gradle/libs.versions.toml file into test project ${testDir.name}"
        from(rootProject.rootDir.resolve("gradle/libs.versions.toml"))
        destinationDir = testDir.resolve("gradle")
        tasks.getByName("processTestResources").dependsOn(this)
        tasks.withType(Cpd::class.java) { dependsOn(this@create) }
    }
}
