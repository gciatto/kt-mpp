package io.github.gciatto.kt.mpp

interface DefaultProperties {

    val disableJavadocTask: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "disableJavadocTask",
            description = "If true, default javadoc task will be disabled",
            mandatory = false,
            defaultValue = true,
        )
    val allWarningsAsErrors: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "allWarningsAsErrors",
            description = "If true, the Kotlin compiler will consider all warnings as errors",
            mandatory = false,
            defaultValue = true,
        )

    val developerIdEmail: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "developer<ID>Email",
            description = "The email of developer <ID> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val developerIdName: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "developerIdName",
            description = "The full name of developer <ID> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val developerIdOrg: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "developerIdOrg",
            description = "Reference to the organization <ORG> of developer <ID> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val developerIdUrl: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "developerIdUrl",
            description = "The homepage URL of developer <ID> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val issuesEmail: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "issuesEmail",
            description = "Issue tracking email (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val issuesUrl: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "issuesUrl",
            description = "Issue tracking web page URL (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val ktCompilerArgs: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "ktCompilerArgs",
            description = "Free compiler arguments to be passed to the Kotlin compiler, for all platforms",
            mandatory = true,
            defaultValue = "",
        )

    val ktCompilerArgsJs: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "ktCompilerArgsJs",
            description = "Free compiler arguments to be passed to the Kotlin compiler when compiling JS sources",
            mandatory = true,
            defaultValue = "",
        )

    val ktCompilerArgsJvm: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "ktCompilerArgsJvm",
            description = "Free compiler arguments to be passed to the Kotlin compiler when compiling JVM sources",
            mandatory = true,
            defaultValue = "",
        )

    val ktTargetJsDisable: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "ktTargetJsDisable",
            description = "If true, disables the JS target on a multi-platform project",
            mandatory = false,
            defaultValue = false,
        )

    val ktTargetJvmDisable: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "ktTargetJvmDisable",
            description = "If true, disables the JVM target on a multi-platform project",
            mandatory = false,
            defaultValue = false,
        )

    val repoOwner: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "repoOwner",
            description = "Name of the GitHub user/organization owning the repository of this project. " +
                "Setting this property will assign default values to the following properties in case they " +
                "are unset/blank: <projectHomepage>, <scmUrl>, and <scmConnection>",
            mandatory = false,
            defaultValue = null,
        )

    val mavenCentralPassword: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "mavenCentralPassword",
            description = "The password of the user willing to release Maven publications on Maven Central",
            mandatory = false,
            defaultValue = null,
        )

    val mavenCentralUsername: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "mavenCentralUsername",
            description = "The username of the user willing to release Maven publications on Maven Central",
            mandatory = false,
            defaultValue = null,
        )

    val otherMavenRepo: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "otherMavenRepo",
            description = "The URL of Maven repository upon which Maven publications will be released",
            mandatory = false,
            defaultValue = "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
        )

    val otherMavenPassword: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "otherMavenPassword",
            description = "The password of the user willing to release Maven publications on <mavenRepo>",
            mandatory = false,
            defaultValue = null,
        )

    val otherMavenUsername: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "otherMavenUsername",
            description = "The username of the user willing to release Maven publications on <mavenRepo>",
            mandatory = false,
            defaultValue = null,
        )

    val mochaTimeout: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "mochaTimeout",
            description = "The amount of time to be ",
            mandatory = true,
            defaultValue = "180s",
        )

    val npmDryRun: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "npmDryRun",
            description = "If true, release of NPM packages will simply be simulated (i.e., no actual release)",
            mandatory = false,
            defaultValue = false,
        )

    val npmOrganization: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "npmOrganization",
            description = "If non-blank, Kotlin JS projects will be released as NPM packages named " +
                "@<npmOrganization>/<rootProject.name>-<project.name>, otherwise the package name will simply " +
                "be <rootProject.name>-<project.name>",
            mandatory = false,
            defaultValue = "",
        )

    val npmRepo: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "npmRepo",
            description = "The URL of NPM registry upon which NPM publications will be released." +
                "If missing or blank, https://registry.npmjs.org will be used",
            mandatory = false,
            defaultValue = "https://registry.npmjs.org",
        )

    val npmToken: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "npmToken",
            description = "The authentication token of the user willing to release NPM publications on <npmRepo>",
            mandatory = false,
            defaultValue = null,
        )

    val orgName: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "<ORG>Name",
            description = "The full name of organization <ORG> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val orgUrl: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "<ORG>Url",
            description = "The URL of the homepage of organization <ORG> (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val projectDescription: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "projectDescription",
            description = "Full project description (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val projectHomepage: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "projectHomepage",
            description = "The URL of the project homepage (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val projectLicense: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "projectLicense",
            description = "Acronym of the license of this project (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = "Apache-2.0",
        )

    val projectLicenseUrl: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "projectLicenseUrl",
            description = "URL of the license of this project (useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = "https://www.apache.org/licenses/LICENSE-2.0",
        )

    val projectLongName: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "projectLongName",
            description = "Non-necessarily path-compliant project name (to be used in place of project.name for " +
                "Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val scmConnection: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "scmConnection",
            description = "The connection string for the DVCS repository hosting the code of this project" +
                "(useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val scmUrl: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "scmUrl",
            description = "The URL of the DVCS repository hosting the code of this project " +
                "(useful for Maven/NPM publications)",
            mandatory = false,
            defaultValue = null,
        )

    val signingKey: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "signingKey",
            description = "The ASCII-armored value of the private key to be used for signing Maven publications." +
                "It should be provided along with <signingPassword>." +
                "If missing or blank, publication artifact signing will be disabled",
            mandatory = false,
            defaultValue = "",
        )

    val signingPassword: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "signingPassword",
            description = "The passphrase of the private key to be used for signing Maven publications." +
                "It should be provided along with <signingPassword>." +
                "If missing or blank, publication artifact signing will be disabled",
            mandatory = false,
            defaultValue = "",
        )

    val versionsFromCatalog: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "versionsFromCatalog",
            description = "The name of the catalog from which Kotlin, JVM, and Node versions should be taken." +
                "Leave empty in case all declared catalogs should be considered, as well as if no one should",
            mandatory = false,
            defaultValue = "",
        )

    val nodeVersion: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "nodeVersion",
            description = "The version of NodeJS to use for running Kotlin JS scripts",
            mandatory = false,
            defaultValue = "",
        )

    val docStyle: PropertyDescriptor
        get() = PropertyDescriptor(
            name = "docStyle",
            description = "The Dokka style to be used for Maven publications" +
                " (one of {'html', 'gfm', 'javadoc', 'jekyll'})",
            mandatory = false,
            defaultValue = "html",
        )
}
