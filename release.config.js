var staging = "-PstagingRepositoryId=${process.env.STAGING_REPO_ID}"
var version = "-PforceVersion=${process.env.ENFORCE_VERSION}"
var gradle = "-Pgradle.publish.key${process.env.GRADLE_PUBLISH_KEY} -Pgradle.publish.secret=${process.env.GRADLE_PUBLISH_SECRET}"

var publishCmd = `
./gradlew ${version} ${staging} releaseStagingRepositoryOnMavenCentral || exit 3
./gradlew ${version} ${gradle} publishPlugins || exit 2
./gradlew ${version} publishAllPublicationsToGithubRepository || true
`
var config = require('semantic-release-preconfigured-conventional-commits');
config.plugins.push(
    [
        "@semantic-release/exec",
        {
            "publishCmd": publishCmd,
        }
    ],
    "@semantic-release/github",
    "@semantic-release/git",
)
module.exports = config
