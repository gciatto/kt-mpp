package io.github.gciatto.kt.mpp.utils

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("MemberVisibilityCanBePrivate")
object JvmVersions {
    fun String.toJavaVersion(): JavaVersion =
        JavaVersion.toVersion(this)

    fun Number.toJavaVersion(): JavaVersion =
        JavaVersion.toVersion(toString())

    fun JavaVersion.toJvmTarget(): JvmTarget {
        val major = ordinal + 1
        return when {
            (major < 8) -> error("Java version $this has no corresponding JvmTarget")
            (major == 8) -> JvmTarget.JVM_1_8
            else -> JvmTarget.valueOf("JVM_$major")
        }
    }

    fun String.toJvmTarget() = toJavaVersion().toJvmTarget()

    fun Number.toJvmTarget() = toJavaVersion().toJvmTarget()
}
