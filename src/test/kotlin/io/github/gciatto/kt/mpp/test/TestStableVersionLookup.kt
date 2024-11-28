package io.github.gciatto.kt.mpp.test

import io.github.gciatto.kt.mpp.utils.StableVersion
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class TestStableVersionLookup : AnnotationSpec() {
    @Test
    fun `Version numbers are found in file`() {
        val text = this::class.java.getResourceAsStream("node-versions.html")?.bufferedReader()?.readText()
            ?: error("Missing resource file: node-versions.html")
        val versions = StableVersion.parseAll(text).toSet()
        versions.min() shouldBe StableVersion(0, 0, 1)
        versions.max() shouldBe StableVersion(23, 3, 0)
    }
}
