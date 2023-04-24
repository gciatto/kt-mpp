package io.github.gciatto.test0

/**
 * The JVM platform singleton: it is only present in JVM packages.
 */
object JsPlatform {
    val js: String
        get() = "js"
}
