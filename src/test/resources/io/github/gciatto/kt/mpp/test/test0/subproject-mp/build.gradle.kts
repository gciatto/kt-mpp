plugins {
    id("io.github.gciatto.kt-mpp.maven-publish")
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":subproject-jvm"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":subproject-js"))
            }
        }
    }
}
