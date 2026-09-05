package io.github.gciatto.kt.mpp.test

import io.github.gciatto.kt.mpp.utils.StableVersion
import io.github.gciatto.kt.mpp.utils.isCacheFresh
import io.github.gciatto.kt.mpp.utils.readVersionsCache
import io.github.gciatto.kt.mpp.utils.retryWithBackoff
import io.github.gciatto.kt.mpp.utils.writeVersionsCache
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory

class TestNodeVersionsCache : AnnotationSpec() {
    private lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        tempDir = createTempDirectory("node-versions-cache-test").toFile()
    }

    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `cache round-trips a version set`() {
        val file = File(tempDir, "nested/node-dist-cache.txt")
        val versions = setOf(StableVersion(22, 9, 0), StableVersion(20, 1, 2))
        writeVersionsCache(file, versions)
        readVersionsCache(file) shouldBe versions
    }

    @Test
    fun `missing or empty cache reads as null`() {
        readVersionsCache(File(tempDir, "missing.txt")) shouldBe null
        val empty = File(tempDir, "empty.txt").apply { writeText("no versions here") }
        readVersionsCache(empty) shouldBe null
    }

    @Test
    fun `freshness is based on the TTL and last-modified time`() {
        val file = File(tempDir, "cache.txt").apply { writeText("1.0.0") }
        val written = file.lastModified()
        isCacheFresh(file, ttlMillis = 1_000, now = written + 500) shouldBe true
        isCacheFresh(file, ttlMillis = 1_000, now = written + 5_000) shouldBe false
    }

    @Test
    fun `retryWithBackoff retries until success without real sleeping`() {
        var calls = 0
        val delays = mutableListOf<Long>()
        val result =
            retryWithBackoff(attempts = 3, initialDelayMillis = 100, sleep = { delays.add(it) }) {
                calls++
                if (calls < 3) error("boom") else "ok"
            }
        result shouldBe "ok"
        calls shouldBe 3
        delays shouldBe listOf(100L, 200L)
    }

    @Test
    fun `retryWithBackoff gives up after exhausting attempts`() {
        var calls = 0
        val error =
            runCatching {
                retryWithBackoff<String>(attempts = 2, initialDelayMillis = 1, sleep = {}) {
                    calls++
                    error("always fails")
                }
            }.exceptionOrNull()
        calls shouldBe 2
        error?.message shouldBe "always fails"
    }
}
