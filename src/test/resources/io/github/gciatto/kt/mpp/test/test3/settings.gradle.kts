rootProject.name = "test3"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs2") {
            from(files("gradle/libs2.versions.toml"))
        }
    }
}
