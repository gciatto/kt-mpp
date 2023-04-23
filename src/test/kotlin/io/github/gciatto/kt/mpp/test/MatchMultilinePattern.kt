package io.github.gciatto.kt.mpp.test

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

class MatchMultilinePattern(private val pattern: String) : Matcher<String> {
    private val regex: Regex by lazy { pattern.toRegex(RegexOption.MULTILINE) }

    override fun test(value: String): MatcherResult = result(regex.find(value) != null)

    private fun result(success: Boolean): MatcherResult = MatcherResult(
        passed = success,
        failureMessageFn = { "Output does not contain pattern $regex" },
        negatedFailureMessageFn = { "Output contains pattern $regex, while it shouldn't" },
    )
}
