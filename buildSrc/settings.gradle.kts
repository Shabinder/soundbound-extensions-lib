dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("../gradle/deps.versions.toml"))
        }
    }
}