import io.gciatto.kt.mpp.ProjectType

plugins {
    id("io.gciatto.kt-mpp.multi-project-helper")
}

group = "io.gciatto.example"
version = "1.0.0"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    jvmProjects(":subproject-jvm")
    jsProjects(":subproject-js")
    otherProjects(":subproject-other")
    applyProjectTemplates()
}
