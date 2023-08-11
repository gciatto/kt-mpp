import io.github.gciatto.kt.mpp.ProjectType
import io.github.gciatto.kt.mpp.log
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.kotlin.dsl.withType
import org.gradle.api.logging.LogLevel

plugins {
    id("io.github.gciatto.kt-mpp.multi-project-helper")
}

group = "io.github.gciatto"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    applyProjectTemplates()
}
