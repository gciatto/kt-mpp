# Kotlin Multi-Platform and Multi-Project Helpers

A bundle of Gradle plugins for multi-platform and -project (MPP, henceforth) Kotlin projects.

Plugins automate and hide most of the boilerplate required to set up MPP Kotlin projects where:
* some sub-projects are multi-platform
* some sub-projects are platform specific
* some sub-projects are not even Kotlin related

For all Kotlin-related (sub-)projects (i.e. multi-platform, or platform specific) pre-configured plugins 
are available to serve the following use cases:
* Kotlin, JVM, JS version declaration and alignment
* Kotlin sources linting (via KtLint)
* Kotlin sources bug finding (via Detekt)
* Kotlin documentation generation (via Dokka)
* Maven artifacts publishing
* NPM packages publishing (in case the JS platform is involved)

There are two supported usage modes (non mutually-exclusive):
* selectively declare plugins on a per-(sub-)project basis
* declare (sub-)projects as multiplatform, JVM-only, JS-only, or other, in the root project's
    `build.gradle.(kts)` file, possibly customising which and how many plugins to apply in each case.

Both usages modes are described below.
In both cases, the behaviour of plugins is regulated by several Gradle properties, 
discussed in the following.

## Available plugins

- `io.github.gciatto.kt-mpp.`__`multiplatform`__: configures a (sub-)project as Kotlin multi-platform, i.e. to support both compilation 
    on JVM and JS

- `io.github.gciatto.kt-mpp.`__`jvm-only`__: configures a (sub-)project as Kotlin JVM only
 
- `io.github.gciatto.kt-mpp.`__`js-only`__: configures a (sub-)project as Kotlin JS only

- `io.github.gciatto.kt-mpp.`__`bug-finder`__: configures Detekt for a Kotlin multiplatform/JVM/JS (sub)-project

- `io.github.gciatto.kt-mpp.`__`documentation`__: configures Dokka for a Kotlin multiplatform/JVM/JS (sub)-project

- `io.github.gciatto.kt-mpp.`__`linter`__: configures KtLint for a Kotlin multiplatform/JVM/JS (sub)-project

- `io.github.gciatto.kt-mpp.`__`maven-publish`__: configures a Kotlin multiplatform/JVM/JS (sub)-project for publishing 
    artifacts on a Maven repository

- `io.github.gciatto.kt-mpp.`__`npm-publish`__: configures a Kotlin multiplatform/JS (sub)-project for publishing
    artifacts on some NPM registry 

- `io.github.gciatto.kt-mpp.`__`versions`__: configures all sub-projects to inherit the `group` and `version`
    metadata from the root project, and provides tasks for printing a (sub-)project's version

- `io.github.gciatto.kt-mpp.`__`multi-project-helper`__: provides facilities to quickly apply subsets of the 
    aforementioned plugins to sub-projects, from the root project (see next section)

## Configuring sub-projects from the root project

This usage mode is made possible by the `multi-project-helper` plugin.

Below an example of root project's `build.gradle.kts` file is reported supporting this usage mode:
```kotlin
import io.github.gciatto.kt.mpp.Plugins
import io.github.gciatto.kt.mpp.ProjectType

plugins {
    id("io.github.gciatto.kt-mpp.multi-project-helper")
}

// these will be inherited by sub-projects!
group = "io.github.username.example"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN // default project type for all projects which are not explicitly marked
    
    jvmProjects(":subproject1-jvm", ":subproject2-jvm") // marks projects as JVM-only
    jsProjects(":subproject-js") // marks projects as JS-only
    otherProjects(":subproject-other") // marks projects as non-Kotlin related
    
    // this would mark projects are multiplatform, but it is not required because all projects...
    // ... which are not explicitly marked are implicitly marked as defaultProjectType
    // ktProjects(rootProject.path, ":subproject-multiplatform")

    // declares plugins to be applied to multiplatform projects
    ktProjectTemplate = buildSet {
        add(Plugins.multiplatform)
        add(Plugins.documentation)
        add(Plugins.linter)
        add(Plugins.bugFinder)
        add(Plugins.versions)
    }

    // declares plugins to be applied to JVM-only projects
    jvmProjectTemplate = buildSet {
        add(Plugins.jvmOnly)
        add(Plugins.documentation)
        add(Plugins.linter)
        add(Plugins.bugFinder)
        add(Plugins.versions)
    }

    // declares plugins to be applied to JS-only projects
    jsProjectTemplate = buildSet {
        add(Plugins.jsOnly)
        add(Plugins.documentation)
        add(Plugins.linter)
        add(Plugins.bugFinder)
        add(Plugins.versions)
    }
    
    // declares plugins to be applied to other projects
    otherProjectTemplate = buildSet {
        add(Plugins.versions)
    }
    
    // actually triggers plugins application for all the projects (implicitly or explicitly) marked so far
    applyProjectTemplates()
    // IMPORTANT: if you forget to invoke this method, no plugin will be applied!
}
```

## Versions Alignment

The plugin will look for a Gradle catalog containing the following version definitions:

- `kotlin` referencing the version of the Kotlin libraries to be used as a dependencies in the projects

- `jvm` referencing the version to be used as target/source compatibility when compiling Kotlin to JVM

- `node` referencing the version of NodeJs  to be used for running the generated JS code
    * version keywords of the form `<major>-latest` are admissible here (e.g., `18-latest`)
    * the version declared in the catalog may be temporarily overridden by assigning the `nodeVersion` 
        property to a non-blank value

So, adding a version catalog to your project is recommended.
The minimal setup may include the file `gradle/libs.versions.toml` in your project's root directory, e.g.:
```toml
[versions]
kotlin = "1.8.10"
jvm = "11"
node = "18-latest"
```

Versions may also be assigned explicitly into (sub-)projects `build.gradle.kts` files via the following extension methods which are made available by this bundle:
- `fun Project.kotlinVersion(provider: String|Provider<String>)`
- `fun Project.jvmVersion(provider: String|Provider<String>)`
- `fun Project.nodeVersion(default: String|Provider<String>, override: Any? = null)`

## Relevant Gradle properties

Consider use tasks 
- `:explainProperties` to get details about the properties acutally needed by a (sub-)project
- `:generateGradlePropertiesFile` automatically generate a `gradle.properties` file for the current (sub-)project

Overall, you may need to define, provide the following properties:

- `allWarningsAsErrors` (optional, default value: `true`): if true, the Kotlin compiler will consider all warnings as errors.

- `disableJavadocTask` (optional, default value: `true`): if true, default javadoc task will be disabled.

- `ktCompilerArgs` (mandatory, default value: `""`): free compiler arguments to be passed to the Kotlin compiler, for all platforms.

- `ktCompilerArgsJvm` (mandatory, default value: `""`): free compiler arguments to be passed to the Kotlin compiler when compiling JVM sources.

- `ktCompilerArgsJs` (mandatory, default value: `""`): free compiler arguments to be passed to the Kotlin compiler when compiling JS sources.

- `mochaTimeout` (mandatory, default value: `"180s"`): the amount of time to be .

- `ktTargetJvmDisable` (optional, default value: `false`): if true, disables the JVM target on a multi-platform project.

- `ktTargetJsDisable` (optional, default value: `false`): if true, disables the JS target on a multi-platform project.

- `versionsFromCatalog` (optional, default value: `""`): the name of the catalog from which Kotlin, JVM, and Node versions should be taken.Leave empty in case all declared catalogs should be considered, as well as if no one should..

- `nodeVersion` (optional, default value: `""`): the version of NodeJS to use for running Kotlin JS scripts.

- `docStyle` (optional, default value: `"html"`): the Dokka style to be used for Maven publications (one of {`"html"`, `"gfm"`, `"javadoc"`, `"jekyll"`}).

- `repoOwner` (optional): name of the GitHub user/organization owning the repository of this project. Setting this property will assign default values to the following properties in case they are unset/blank: `<projectHomepage>`, `<scmUrl>`, and `<scmConnection>`.

- `mavenCentralPassword` (optional): the password of the user willing to release Maven publications on Maven Central.

- `mavenCentralUsername` (optional): the username of the user willing to release Maven publications on Maven Central.

- `otherMavenRepo` (optional, default value: `"""`): the URL of Maven repository upon which Maven publications will be released.

- `otherMavenUsername` (optional): the username of the user willing to release Maven publications on `<mavenRepo>`.

- `otherMavenPassword` (optional): the password of the user willing to release Maven publications on `<mavenRepo>`.

- `signingKey` (optional, default value: `""`): the ASCII-armored value of the private key to be used for signing Maven publications.It should be provided along with `<signingPassword>`. If missing or blank, publication artifact signing will be disabled.

- `signingPassword` (optional, default value: `""`): the passphrase of the private key to be used for signing Maven publications.It should be provided along with `<signingPassword>`. If missing or blank, publication artifact signing will be disabled.

- `projectLongName` (optional): non-necessarily path-compliant project name (to be used in place of project.name for Maven/NPM publications).

- `projectDescription` (optional): full project description (useful for Maven/NPM publications).

- `projectHomepage` (optional): the URL of the project homepage (useful for Maven/NPM publications).

- `projectLicense` (optional, default value: `"Apache-2.0"`): acronym of the license of this project (useful for Maven/NPM publications).

- `projectLicenseUrl` (optional, default value: `"https://www.apache.org/licenses/LICENSE-2.0"`): the URL of the license of this project (useful for Maven/NPM publications).

- `scmConnection` (optional): the connection string for the DVCS repository hosting the code of this project(useful for Maven/NPM publications).

- `scmUrl` (optional): the URL of the DVCS repository hosting the code of this project (useful for Maven/NPM publications).

- `developerIdName` (optional): the full name of developer `<ID>` (useful for Maven/NPM publications).

- `developerIdUrl` (optional): the homepage URL of developer `<ID>` (useful for Maven/NPM publications).

- `developer<ID>Email` (optional): the email of developer `<ID>` (useful for Maven/NPM publications).

- `developerIdOrg` (optional): reference to the organization `<ORG>` of developer `<ID>` (useful for Maven/NPM publications).

- `<ORG>Name` (optional): the full name of organization `<ORG>` (useful for Maven/NPM publications).

- `<ORG>Url` (optional): the URL of the homepage of organization `<ORG>` (useful for Maven/NPM publications).

- `npmOrganization` (optional, default value: `""`): if non-blank, Kotlin JS projects will be released as NPM packages named `@<npmOrganization>/<rootProject.name>-<project.name>`, otherwise the package name will simply be `<rootProject.name>-<project.name>`.

- `npmDryRun` (optional, default value: false): if true, release of NPM packages will simply be simulated (i.e., no actual release).

- `npmRepo` (optional, default value: `"https://registry.npmjs.org"`): the URL of NPM registry upon which NPM publications will be released.If missing or blank, https://registry.npmjs.org will be used.

- `npmToken` (optional): the authentication token of the user willing to release NPM publications on `npmRepo`.

- `issuesUrl` (optional): issue tracking web page URL (useful for Maven/NPM publications).

- `issuesEmail` (optional): issue tracking email (useful for Maven/NPM publications).

- `dokkaArtifactInMavenPublication` (optional, default value: `"html"`): the Dokka artifact type to be used for Maven publications (one of {`"html"`, `gfm`, `javadoc`, `jekyll`})

## How to use

1. Create a `gradle/libs.versions.toml` file in your project's root directory, e.g.:

    ```yaml
    [versions]
    kotlin = "1.8.10"
    jvm = "1.8"
    node = "16-latest"
    ktMpp = "<VERSION_HERE>"
    
    [libraries]
    kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect", version.ref = "kotlin" }
    
    [plugins]
    ktMpp-bugFinder = { id = "io.github.gciatto.kt-mpp.bug-finder", version.ref = "ktMpp" }
    ktMpp-documentation = { id = "io.github.gciatto.kt-mpp.documentation", version.ref = "ktMpp" }
    ktMpp-linter = { id = "io.github.gciatto.kt-mpp.linter", version.ref = "ktMpp" }
    ktMpp-mavenPublish = { id = "io.github.gciatto.kt-mpp.maven-publish", version.ref = "ktMpp" }
    ktMpp-npmPublish = { id = "io.github.gciatto.kt-mpp.npm-publish", version.ref = "ktMpp" }
    ktMpp-versions = { id = "io.github.gciatto.kt-mpp.versions", version.ref = "ktMpp" }
    ktMpp-jsOnly = { id = "io.github.gciatto.kt-mpp.js-only", version.ref = "ktMpp" }
    ktMpp-jvmOnly = { id = "io.github.gciatto.kt-mpp.jvm-only", version.ref = "ktMpp" }
    ktMpp-multiplatform = { id = "io.github.gciatto.kt-mpp.multiplatform", version.ref = "ktMpp" }
    ktMpp-multiProjectHelper = { id = "io.github.gciatto.kt-mpp.multi-project-helper", version.ref = "ktMpp" }
    ```
   
2. Fill your root project's `build.gradle.kts` file as follows:

    ```kotlin
    @Suppress("DSL_SCOPE_VIOLATION")
    plugins {
        alias(libs.plugins.ktMpp.<SPECIFIC PLUGIN HERE>)
    }
    ```
