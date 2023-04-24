package io.github.gciatto.test0

/**
 * The JVM platform singleton: it is only present in JVM packages.
 */
object JvmPlatform {
    val jvm: String
        get() = "jvm"
}
