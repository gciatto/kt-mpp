package io.github.gciatto.kt.mpp.test.test0.full.src.commonTest.kotlin.io.github.gciatto.test0

import io.github.gciatto.test0.currentPlatformMessage
import kotlin.test.Test
import kotlin.test.assertTrue

class Test {
    private val admissibles = listOf("jvm-java", "js").map { "Current platform: $it" }.toSet()
    @Test
    fun test() {
        val current = currentPlatformMessage
        assertTrue(
            current in admissibles,
            "Actual '$current', admissible: ${admissibles.joinToString { "'$it'" }}"
        )
    }
}
