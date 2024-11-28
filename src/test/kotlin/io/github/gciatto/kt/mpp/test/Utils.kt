package io.github.gciatto.kt.mpp.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

fun String.shouldBeVersionNumber() {
    val pieces = split(".")
    val numbers = pieces.map { it.toIntOrNull() }
    numbers.size shouldBe 3
    for (n in numbers) {
        n shouldNotBe null
    }
}
