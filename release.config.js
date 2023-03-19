var publishCmd = `
./gradlew uploadKotlinOSSRHToMavenCentralNexus uploadPluginMavenToMavenCentralNexus uploadAllPluginMarkersToMavenCentralNexus release || exit 1
./gradlew publishPlugins -Pgradle.publish.key=$GRADLE_PUBLISH_KEY -Pgradle.publish.secret=$GRADLE_PUBLISH_SECRET || exit 2
./gradlew publishAllPublicationsToGithubRepository || true
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
