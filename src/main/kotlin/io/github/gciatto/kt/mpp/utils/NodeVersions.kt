package io.github.gciatto.kt.mpp.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object NodeVersions {
    private const val NODE_DIST_URL = "https://nodejs.org/dist"

    private val VERSIONS: Set<StableVersion> by lazy {
        val reader = BufferedReader(InputStreamReader(NODE_DIST_URL.toURL().openStream()))
        StableVersion.parseAll(reader).toSet()
    }

    private val VERSIONS_CACHE = mutableMapOf<String, String>()

    private val MAJOR_REGEX = "^(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val MAJOR_MINOR_REGEX = "^(\\d+)\\.(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val FULL_VERSION_REGEX = "^(\\d+)\\.(\\d+)\\.(\\d+)$".toRegex()
    private val LATEST_VERSION_REGEX = "^v?(\\d+)-latest|latest-v?(\\d+)$".toRegex(RegexOption.IGNORE_CASE)

    @Suppress("NAME_SHADOWING")
    private fun findLatestVersion(version: String): StableVersion? {
        val version = version.trim()
        if (version.equals("latest", ignoreCase = true)) {
            return VERSIONS.max()
        }
        val match =
            MAJOR_REGEX.matchEntire(version)
                ?: LATEST_VERSION_REGEX.matchEntire(version)
                ?: MAJOR_MINOR_REGEX.matchEntire(version)
                ?: FULL_VERSION_REGEX.matchEntire(version)
                ?: return null
        val numbers = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
        val minVersion = StableVersion.of(numbers)
        val upperBound = minVersion.nextMajor()
        return VERSIONS.filter { it >= minVersion && it < upperBound }.maxOrNull().also {
            println(it)
        }
    }

    fun latest(version: String = "latest"): String =
        VERSIONS_CACHE.computeIfAbsent(version) {
            findLatestVersion(it)?.toVersionString() ?: error("No such node version: $version")
        }
}
