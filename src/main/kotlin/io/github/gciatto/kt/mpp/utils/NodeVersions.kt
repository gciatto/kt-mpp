package io.github.gciatto.kt.mpp.utils

import org.gradle.api.logging.Logging
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

internal fun isCacheFresh(
    file: File,
    ttlMillis: Long,
    now: Long = System.currentTimeMillis(),
): Boolean = file.isFile && now - file.lastModified() < ttlMillis

internal fun readVersionsCache(file: File): Set<StableVersion>? =
    runCatching { StableVersion.parseAll(file.readText()).toSet() }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() }

internal fun writeVersionsCache(
    file: File,
    versions: Set<StableVersion>,
) {
    file.parentFile?.mkdirs()
    val tmp = File.createTempFile("node-dist-cache", ".tmp", file.parentFile)
    tmp.writeText(versions.joinToString("\n") { it.toVersionString() })
    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
}

@Suppress("TooGenericExceptionCaught")
internal fun <T> retryWithBackoff(
    attempts: Int,
    initialDelayMillis: Long,
    sleep: (Long) -> Unit = Thread::sleep,
    onRetry: (attempt: Int, error: Throwable) -> Unit = { _, _ -> },
    block: () -> T,
): T {
    require(attempts > 0) { "attempts must be positive" }
    var delay = initialDelayMillis
    for (attempt in 1..attempts) {
        try {
            return block()
        } catch (e: Exception) {
            onRetry(attempt, e)
            if (attempt == attempts) throw e
            sleep(delay)
            delay *= 2
        }
    }
    error("unreachable")
}

object NodeVersions {
    private const val NODE_DIST_URL = "https://nodejs.org/dist"
    private const val CONNECT_TIMEOUT_MILLIS = 5_000
    private const val READ_TIMEOUT_MILLIS = 5_000
    private const val MAX_ATTEMPTS = 3
    private const val INITIAL_BACKOFF_MILLIS = 500L
    private const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L

    private val logger = Logging.getLogger(NodeVersions::class.java)

    @Volatile
    private var cacheFile: File? = null

    private val VERSIONS: Set<StableVersion> by lazy { loadVersions() }

    private val VERSIONS_CACHE = ConcurrentHashMap<String, String>()

    private val MAJOR_REGEX = "^(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val MAJOR_MINOR_REGEX = "^(\\d+)\\.(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val FULL_VERSION_REGEX = "^(\\d+)\\.(\\d+)\\.(\\d+)$".toRegex()
    private val LATEST_VERSION_REGEX = "^v?(\\d+)-latest|latest-v?(\\d+)$".toRegex(RegexOption.IGNORE_CASE)

    private fun loadVersions(): Set<StableVersion> {
        val file = cacheFile
        if (file != null && isCacheFresh(file, CACHE_TTL_MILLIS)) {
            readVersionsCache(file)?.let { return it }
        }
        val fetched =
            runCatching {
                retryWithBackoff(
                    MAX_ATTEMPTS,
                    INITIAL_BACKOFF_MILLIS,
                    onRetry = { attempt, error ->
                        logger.warn(
                            "Attempt $attempt/$MAX_ATTEMPTS to fetch Node version list " +
                                "from $NODE_DIST_URL failed: ${error.message}",
                        )
                    },
                ) { fetchVersions() }
            }
        fetched.getOrNull()?.let { versions ->
            val target = file
            if (target != null) {
                runCatching { writeVersionsCache(target, versions) }
                    .onFailure { error -> logger.warn("Failed to write Node version cache to $target", error) }
            }
            return versions
        }
        if (file != null) {
            readVersionsCache(file)?.let {
                logger.warn(
                    "Failed to fetch Node version list from $NODE_DIST_URL; falling back to cached copy at $file",
                    fetched.exceptionOrNull(),
                )
                return it
            }
        }
        throw fetched.exceptionOrNull() ?: error("Failed to fetch Node version list from $NODE_DIST_URL")
    }

    private fun fetchVersions(): Set<StableVersion> {
        val connection = NODE_DIST_URL.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        check(connection.responseCode == HttpURLConnection.HTTP_OK) {
            "Unexpected HTTP status ${connection.responseCode} from $NODE_DIST_URL"
        }
        val versions =
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                StableVersion.parseAll(reader).toSet()
            }
        check(versions.isNotEmpty()) { "Parsed 0 Node versions from $NODE_DIST_URL" }
        return versions
    }

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
        return VERSIONS.filter { it in minVersion..<upperBound }.maxOrNull()
    }

    /**
     * @param cacheFile where to persist the parsed Node version list (e.g. rootProject's build dir),
     * so that repeated builds/CI jobs can avoid re-fetching https://nodejs.org/dist. Only the first
     * call that supplies one takes effect, since the fetched version list itself is cached for the
     * lifetime of this object (i.e. of the Gradle daemon).
     */
    fun latest(
        version: String = "latest",
        cacheFile: File? = null,
    ): String {
        cacheFile?.let { this.cacheFile = it }
        return VERSIONS_CACHE.computeIfAbsent(version) {
            findLatestVersion(it)?.toVersionString() ?: error("No such node version: $version")
        }
    }
}
