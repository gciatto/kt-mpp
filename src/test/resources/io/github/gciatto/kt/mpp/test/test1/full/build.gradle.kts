kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":subproject-mp"))
            }
        }
    }
}
