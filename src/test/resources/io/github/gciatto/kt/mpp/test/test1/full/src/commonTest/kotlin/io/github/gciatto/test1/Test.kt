package io.github.gciatto.kt.mpp.test.test1

import io.github.gciatto.test0.currentPlatformMessage
import kotlin.test.Test
import kotlin.test.assertTrue

class Test {
    private val admissibles = listOf("jvm", "js").map { "Current platform: $it" }.toSet()
    @Test
    fun test() {
        val current = currentPlatformMessage
        assertTrue(
            current in admissibles,
            "Actual '$current', admissible: ${admissibles.joinToString { "'$it'" }}"
        )
    }
}
