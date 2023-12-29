import io.github.gciatto.kt.mpp.helpers.ProjectType

plugins {
    id("io.github.gciatto.kt-mpp.multi-project-helper")
}

group = "io.github.gciatto"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    applyProjectTemplates()
}

allprojects {
    repositories {
        mavenCentral()
    }
}
