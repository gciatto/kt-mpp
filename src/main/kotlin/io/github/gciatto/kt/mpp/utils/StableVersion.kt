package io.github.gciatto.kt.mpp.utils

import java.io.Reader
import java.io.StringReader

data class StableVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<StableVersion> {
    override fun compareTo(other: StableVersion): Int =
        if (major == other.major) {
            if (minor == other.minor) {
                patch - other.patch
            } else {
                minor - other.minor
            }
        } else {
            major - other.major
        }

    fun toVersionString(): String = "$major.$minor.$patch"

    fun nextMajor(): StableVersion = StableVersion(major + 1, 0, 0)

    fun nextMinor(): StableVersion = StableVersion(major, minor + 1, 0)

    fun nextPatch(): StableVersion = StableVersion(major, minor, minor + 1)

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val REGEX = Regex("(\\d+)\\.(\\d+)\\.(\\d+)")

        private fun anyToInt(obj: Any): Int = when (obj) {
            is Int -> obj
            is Number -> obj.toInt()
            is String -> obj.toInt()
            else -> error("Cannot convert $obj to Int")
        }

        @JvmStatic
        fun of(numbers: Iterable<Any>): StableVersion {
            val i = numbers.iterator()
            val major = anyToInt(if (i.hasNext()) i.next() else 0)
            val minor = anyToInt(if (i.hasNext()) i.next() else 0)
            val patch = anyToInt(if (i.hasNext()) i.next() else 0)
            return StableVersion(major, minor, patch)
        }

        @JvmStatic
        fun of(vararg numbers: Any): StableVersion = of(numbers.asIterable())

        @JvmStatic
        fun parseOrNull(version: String?): StableVersion? {
            if (version == null) return null
            val match = REGEX.matchEntire(version) ?: return null
            return of(match.groupValues.drop(1))
        }

        @JvmStatic
        fun parse(version: String): StableVersion = parseOrNull(version) ?: error("Invalid version: $version")

        @JvmStatic
        fun parseAll(reader: Reader): Sequence<StableVersion> = sequence {
            reader.useLines { lines ->
                for (line in lines) {
                    for (match in REGEX.findAll(line)) {
                        yield(of(match.groupValues.drop(1)))
                    }
                }
            }
        }

        @JvmStatic
        fun parseAll(string: String): Sequence<StableVersion> = parseAll(StringReader(string))
    }
}
