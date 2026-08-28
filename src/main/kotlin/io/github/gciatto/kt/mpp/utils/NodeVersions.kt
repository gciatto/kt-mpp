package io.github.gciatto.kt.mpp.utils

import org.gradle.api.Project
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object NodeVersions {
    private const val NODE_DIST_URL = "https://nodejs.org/dist"
    private const val NODE_VERSIONS_CACHE_FILE_NAME = ".node-versions"
    private const val DEFAULT_FETCH_RETRIES = 3
    private const val DEFAULT_FETCH_BACKOFF_MILLIS = 250L
    private const val DEFAULT_FETCH_MAX_BACKOFF_MILLIS = 2000L
    private const val FETCH_RETRIES_PROPERTY = "nodeVersionsFetchRetries"
    private const val FETCH_BACKOFF_MILLIS_PROPERTY = "nodeVersionsFetchBackoffMillis"
    private const val FETCH_MAX_BACKOFF_MILLIS_PROPERTY = "nodeVersionsFetchMaxBackoffMillis"

    private val CACHE_LOCK = Any()

    private val VERSIONS_CACHE = mutableMapOf<String, Set<StableVersion>>()

    private val RESOLUTION_CACHE = mutableMapOf<String, MutableMap<String, String>>()

    private val MAJOR_REGEX = "^(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val MAJOR_MINOR_REGEX = "^(\\d+)\\.(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val FULL_VERSION_REGEX = "^(\\d+)\\.(\\d+)\\.(\\d+)$".toRegex()
    private val LATEST_VERSION_REGEX = "^(?:v?(\\d+)-latest|latest-v?(\\d+))$".toRegex(RegexOption.IGNORE_CASE)

    private data class FetchSettings(
        val retries: Int,
        val initialBackoffMillis: Long,
        val maxBackoffMillis: Long,
    )

    private data class ResolutionContext(
        val rootDir: File,
        val propertyResolver: (String) -> Any?,
    ) {
        val key: String
            get() = rootDir.absolutePath
        val cacheFile: File
            get() = rootDir.resolve(NODE_VERSIONS_CACHE_FILE_NAME)
    }

    private fun fromProject(project: Project) =
        ResolutionContext(project.rootProject.projectDir) { property ->
            project.findProperty(property) ?: project.rootProject.findProperty(property)
        }

    private fun fromCurrentProcess() =
        ResolutionContext(File(System.getProperty("user.dir"))) { property ->
            System.getProperty(property)
        }

    private fun resolveIntProperty(
        context: ResolutionContext,
        name: String,
        default: Int,
        min: Int,
    ): Int =
        context
            .propertyResolver(name)
            ?.toString()
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it >= min }
            ?: default

    private fun resolveLongProperty(
        context: ResolutionContext,
        name: String,
        default: Long,
        min: Long,
    ): Long =
        context
            .propertyResolver(name)
            ?.toString()
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it >= min }
            ?: default

    private fun fetchSettings(context: ResolutionContext): FetchSettings {
        val retries = resolveIntProperty(context, FETCH_RETRIES_PROPERTY, DEFAULT_FETCH_RETRIES, 0)
        val initialBackoff =
            resolveLongProperty(context, FETCH_BACKOFF_MILLIS_PROPERTY, DEFAULT_FETCH_BACKOFF_MILLIS, 1)
        val maxBackoff =
            resolveLongProperty(
                context,
                FETCH_MAX_BACKOFF_MILLIS_PROPERTY,
                DEFAULT_FETCH_MAX_BACKOFF_MILLIS,
                initialBackoff,
            )
        return FetchSettings(retries, initialBackoff, maxBackoff)
    }

    private fun parseVersions(content: String): Set<StableVersion> = StableVersion.parseAll(content).toSet()

    private fun fetchVersionsFromWeb(context: ResolutionContext): Set<StableVersion> {
        val settings = fetchSettings(context)
        var waitMillis = settings.initialBackoffMillis
        var failures: Throwable? = null
        repeat(settings.retries + 1) { attempt ->
            runCatching {
                BufferedReader(InputStreamReader(NODE_DIST_URL.toURL().openStream())).use {
                    StableVersion.parseAll(it).toSet()
                }
            }.onSuccess {
                return it
            }.onFailure {
                if (failures == null) {
                    failures = it
                } else {
                    val previousFailure: Throwable = failures
                    previousFailure.addSuppressed(it)
                }
                if (attempt < settings.retries) {
                    try {
                        Thread.sleep(waitMillis)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw interrupted
                    }
                    waitMillis = (waitMillis * 2).coerceAtMost(settings.maxBackoffMillis)
                }
            }
        }
        val failure = failures ?: error("Unable to fetch Node versions from $NODE_DIST_URL")
        throw failure
    }

    private fun writeCache(
        context: ResolutionContext,
        versions: Set<StableVersion>,
    ) {
        val payload =
            versions
                .sorted()
                .joinToString(System.lineSeparator()) { it.toVersionString() }
                .plus(System.lineSeparator())
        context.cacheFile.writeText(payload)
    }

    private fun loadVersions(
        context: ResolutionContext,
        forceRefresh: Boolean = false,
    ): Set<StableVersion> =
        synchronized(CACHE_LOCK) {
            loadVersionsLocked(context, forceRefresh)
        }

    private fun loadVersionsLocked(
        context: ResolutionContext,
        forceRefresh: Boolean = false,
    ): Set<StableVersion> {
        val loaded =
            if (!forceRefresh) {
                VERSIONS_CACHE[context.key]
                    ?: context.cacheFile
                        .takeIf { it.exists() && it.isFile }
                        ?.readText()
                        ?.let(::parseVersions)
                        ?.takeIf(Set<StableVersion>::isNotEmpty)
            } else {
                null
            }

        val value =
            loaded ?: fetchVersionsFromWeb(context).also { fetched ->
                writeCache(context, fetched)
            }

        VERSIONS_CACHE[context.key] = value
        if (forceRefresh) {
            RESOLUTION_CACHE.remove(context.key)
        }
        return value
    }

    @Suppress("NAME_SHADOWING")
    private fun findLatestVersion(
        version: String,
        versions: Set<StableVersion>,
    ): StableVersion? {
        val version = version.trim()
        if (version.equals("latest", ignoreCase = true)) {
            return versions.max()
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
        return versions.filter { it >= minVersion && it < upperBound }.maxOrNull()
    }

    fun refreshCache(project: Project) {
        val context = fromProject(project)
        synchronized(CACHE_LOCK) {
            loadVersionsLocked(context, forceRefresh = true)
        }
    }

    fun latest(
        project: Project,
        version: String = "latest",
    ): String = latest(fromProject(project), version)

    fun latest(version: String = "latest"): String =
        latest(fromCurrentProcess(), version)

    private fun latest(
        context: ResolutionContext,
        version: String = "latest",
    ): String =
        synchronized(CACHE_LOCK) {
            val versions = loadVersionsLocked(context)
            val cache = RESOLUTION_CACHE.getOrPut(context.key) { mutableMapOf() }
            cache.getOrPut(version) {
                findLatestVersion(version, versions)?.toVersionString() ?: error("No such node version: $version")
            }
        }
}
