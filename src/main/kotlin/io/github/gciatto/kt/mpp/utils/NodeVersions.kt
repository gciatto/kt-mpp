package io.github.gciatto.kt.mpp.utils

import org.gradle.api.logging.Logging
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

private val CACHE_TIMESTAMP_REGEX = "^#\\s*generated-at=(\\d+)\\s*$".toRegex()

/**
 * Reads the generation timestamp embedded as a `# generated-at=<epochMillis>` first line, rather than
 * relying on the file's own last-modified time, since CI caching (e.g. GitHub Actions) may restore the
 * file with a fresh mtime while its content is actually older.
 */
internal fun readCacheTimestamp(file: File): Long? =
    runCatching { file.useLines { it.firstOrNull() } }
        .getOrNull()
        ?.let {
            CACHE_TIMESTAMP_REGEX
                .matchEntire(it)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        }

internal fun isCacheFresh(
    file: File,
    ttlMillis: Long,
    now: Long = System.currentTimeMillis(),
): Boolean {
    val generatedAt = readCacheTimestamp(file) ?: return false
    return now - generatedAt < ttlMillis
}

internal fun readVersionsCache(file: File): Set<StableVersion>? =
    runCatching { StableVersion.parseAll(file.readText()).toSet() }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() }

internal fun writeVersionsCache(
    file: File,
    versions: Set<StableVersion>,
    generatedAt: Long = System.currentTimeMillis(),
) {
    file.parentFile?.mkdirs()
    val tmp = File.createTempFile("node-dist-cache", ".tmp", file.parentFile)
    val content = "# generated-at=$generatedAt\n" + versions.joinToString("\n") { it.toVersionString() }
    tmp.writeText(content)
    try {
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
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

    /**
     * Tunables for fetching/caching the Node version list, overridable via Gradle properties
     * (see [Project.nodeVersion][io.github.gciatto.kt.mpp.utils.nodeVersion]).
     */
    data class FetchConfig(
        val connectTimeoutMillis: Int = 5_000,
        val readTimeoutMillis: Int = 5_000,
        val maxAttempts: Int = 3,
        val initialBackoffMillis: Long = 500L,
        val cacheTtlMillis: Long = 24 * 60 * 60 * 1000L,
    )

    private val logger = Logging.getLogger(NodeVersions::class.java)

    @Volatile
    private var cacheFile: File? = null

    @Volatile
    private var config: FetchConfig? = null

    private val VERSIONS: Set<StableVersion> by lazy { loadVersions() }

    private val VERSIONS_CACHE = ConcurrentHashMap<String, String>()

    private val MAJOR_REGEX = "^(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val MAJOR_MINOR_REGEX = "^(\\d+)\\.(\\d+)(?:\\.[a-zA-Z])?$".toRegex()
    private val FULL_VERSION_REGEX = "^(\\d+)\\.(\\d+)\\.(\\d+)$".toRegex()
    private val LATEST_VERSION_REGEX = "^v?(\\d+)-latest|latest-v?(\\d+)$".toRegex(RegexOption.IGNORE_CASE)

    private fun loadVersions(): Set<StableVersion> {
        val file = cacheFile
        val cfg = config ?: FetchConfig()
        if (file != null && isCacheFresh(file, cfg.cacheTtlMillis)) {
            readVersionsCache(file)?.let { return it }
        }
        val fetched =
            runCatching {
                retryWithBackoff(
                    cfg.maxAttempts,
                    cfg.initialBackoffMillis,
                    onRetry = { attempt, error ->
                        logger.warn(
                            "Attempt $attempt/${cfg.maxAttempts} to fetch Node version list " +
                                "from $NODE_DIST_URL failed: ${error.message}",
                        )
                    },
                ) { fetchVersions(cfg) }
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

    private fun fetchVersions(cfg: FetchConfig): Set<StableVersion> {
        val connection = NODE_DIST_URL.toURL().openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = cfg.connectTimeoutMillis
            connection.readTimeout = cfg.readTimeoutMillis
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Unexpected HTTP status ${connection.responseCode} from $NODE_DIST_URL"
            }
            val versions =
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    StableVersion.parseAll(reader).toSet()
                }
            check(versions.isNotEmpty()) { "Parsed 0 Node versions from $NODE_DIST_URL" }
            return versions
        } finally {
            connection.disconnect()
        }
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
     * @param config tunables for the fetch/cache; only the first call that supplies one takes effect,
     * for the same reason as [cacheFile].
     */
    fun latest(
        version: String = "latest",
        cacheFile: File? = null,
        config: FetchConfig? = null,
    ): String {
        if (this.cacheFile == null) cacheFile?.let { this.cacheFile = it }
        if (this.config == null) config?.let { this.config = it }
        return VERSIONS_CACHE.computeIfAbsent(version.trim()) {
            findLatestVersion(it)?.toVersionString() ?: error("No such node version: $version")
        }
    }
}
