package io.github.gciatto.test1

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
