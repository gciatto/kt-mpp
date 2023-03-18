listOf("jvm", "js", "mp", "other").forEach {
    include("subproject-$it")
}
include("full")

rootProject.name = "test0"
