package io.github.gciatto.test0

/**
 * The Browser platform singleton: it is only present in browser-targeted packages.
 */
object BrowserPlatform {
    val browser: String
        get() = "browser"
}
