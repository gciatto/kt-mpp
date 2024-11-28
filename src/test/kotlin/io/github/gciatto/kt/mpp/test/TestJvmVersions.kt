package io.github.gciatto.kt.mpp.test

import io.github.gciatto.kt.mpp.utils.JvmVersions.toJavaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.JavaVersion

class TestJvmVersions : FunSpec({
    for (i in 1..10) {
        test("Recognize Java version 1.$i") {
            "1.$i".toJavaVersion() shouldBe JavaVersion.valueOf("VERSION_1_$i")
        }
    }
    val maxVersion = JavaVersion.entries
        .map { it.name }
        .map { it.removePrefix("VERSION_") }
        .filterNot { "_" in it }
        .mapNotNull { it.toIntOrNull() }
        .max()
    for (i in 11..maxVersion) {
        test("Recognize Java version $i") {
            i.toJavaVersion() shouldBe JavaVersion.valueOf("VERSION_$i")
        }
    }
})
