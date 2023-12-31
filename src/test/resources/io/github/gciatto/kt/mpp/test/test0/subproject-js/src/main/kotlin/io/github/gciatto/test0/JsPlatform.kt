package io.github.gciatto.test0

import kotlin.js.JsExport

/**
 * The JVM platform singleton: it is only present in JVM packages.
 */
@JsExport
@Suppress("WRONG_EXPORTED_DECLARATION", "NON_EXPORTABLE_TYPE")
object JsPlatform {
    val js: String
        get() = "js"
}
