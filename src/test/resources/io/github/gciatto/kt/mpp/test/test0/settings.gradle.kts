listOf("jvm", "js", "browser", "mp", "other").forEach {
    include("subproject-$it")
}
include("full")

rootProject.name = "test0"
