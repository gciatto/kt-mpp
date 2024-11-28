package io.github.gciatto.kt.mpp.test

import io.github.gciatto.kt.mpp.utils.JvmVersions.toJvmTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class TestJvmTargets : FunSpec({
    test("Recognize JVM target 1.8") {
        "1.8".toJvmTarget() shouldBe JvmTarget.JVM_1_8
    }
    val maxVersion = JvmTarget.entries
        .map { it.name }
        .map { it.removePrefix("JVM_") }
        .filterNot { "_" in it }
        .maxOf { it.toInt() }
    for (i in 9..maxVersion) {
        test("Recognize JVM target $i") {
            i.toJvmTarget() shouldBe JvmTarget.valueOf("JVM_$i")
        }
    }
})
