@file:Suppress("OPT_IN_USAGE")

import org.apache.tools.ant.taskdefs.condition.Os
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
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

/*
 * Project information
 */
group = "io.gciatto"
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

multiJvm {
    jvmVersionForCompilation.set(11)
    maximumSupportedJvmVersion.set(latestJavaSupportedByGradle)
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    api(kotlin("stdlib-jdk8"))
    implementation(libs.kotlin.bom)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.npmPublish)
    implementation(libs.dokka)
    implementation(libs.ktlint)
    implementation(libs.detekt)
    testImplementation(gradleTestKit())
    testImplementation(libs.konf.yaml)
    testImplementation(libs.classgraph)
    testImplementation(libs.bundles.kotlin.testing)
}

// Enforce Kotlin version coherence
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
            useVersion(KOTLIN_VERSION)
            because("All Kotlin modules should use the same version, and compiler uses $KOTLIN_VERSION")
        }
    }
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(tasks.generateJacocoTestKitProperties)
    testLogging {
        showStandardStreams = true
        showCauses = true
        showStackTraces = true
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
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

gradlePlugin {
    plugins {
        website.set(info.website)
        vcsUrl.set(info.vcsUrl)

        fun innerPlugin(
            name: String,
            confName: String = name,
            descr: String,
            klass: String,
            vararg moreTags: String
        ) = create(name) {
            id = "$group.${project.name}.$name"
            displayName = "Default $confName configuration for Kotlin multi-platform projects"
            description = "${project.description}: $descr"
            implementationClass = "${info.pluginImplementationClass}.$klass"
            tags.set(info.tags + listOf(*moreTags))
        }

        innerPlugin(
            name = "kotlin-bug-finder",
            descr = "bug-finder (currently, Detekt)",
            klass = "KotlinBugFinder",
            moreTags = arrayOf("bug-finder", "detekt")
        )

        innerPlugin(
            name = "kotlin-doc",
            descr = "documentation generator (currently, Dokka)",
            klass = "KotlinDoc",
            moreTags = arrayOf("doc", "dokka")
        )

        innerPlugin(
            name = "kotlin-linter",
            descr = "linter (currently, KtLint)",
            klass = "KotlinLinter",
            moreTags = arrayOf("linter", "ktlint")
        )

        innerPlugin(
            name = "maven-publish",
            descr = "maven publication",
            klass = "PublishOnMaven",
            moreTags = arrayOf("maven")
        )

        innerPlugin(
            name = "npm-publish",
            descr = "npm publication",
            klass = "PublishOnNpm",
            moreTags = arrayOf("npm")
        )

        innerPlugin(
            name = "print-versions",
            descr = "version revealer",
            klass = "PrintVersions",
            moreTags = arrayOf("version")
        )

        innerPlugin(
            name = "koltin-js-only",
            descr = "JS only project configuration",
            klass = "KotlinJsOnly",
            moreTags = arrayOf("js")
        )

        innerPlugin(
            name = "koltin-jvm-only",
            descr = "JVM only project configuration",
            klass = "KotlinJvmOnly",
            moreTags = arrayOf("jvm")
        )
    }
}
