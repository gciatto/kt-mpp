import io.gciatto.kt.mpp.ProjectType

plugins {
    id("io.gciatto.kt-mpp.multi-project-helper")
}

version = "1.0.0-example"

multiProjectHelper {
    defaultProjectType = ProjectType.KOTLIN
    jvmProjects(":subproject-jvm")
    jsProjects(":subproject-js")
    otherProjects(":subproject-other")
}
