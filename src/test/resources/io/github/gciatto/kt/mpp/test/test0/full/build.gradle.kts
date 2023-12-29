plugins {
    id("io.github.gciatto.kt-mpp.maven-publish")
    id("io.github.gciatto.kt-mpp.npm-publish")
    id("io.github.gciatto.kt-mpp.fat-jar")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":subproject-mp"))
            }
        }
    }
}
