package io.github.gciatto.kt.mpp.test

import com.uchuhimo.konf.ConfigSpec
import java.io.File
import kotlin.streams.asSequence

object Root : ConfigSpec("") {
    val tests by required<List<Test>>()
    val options by optional(emptyList<String>())
}

data class Test(
    val description: String,
    val configuration: Configuration,
    val expectation: Expectation,
)

data class Configuration(val tasks: List<String>, val options: List<String> = emptyList())

@Suppress("ConstructorParameterNaming")
data class Expectation(
    val file_exists: List<ExistingFile> = emptyList(),
    val success: List<String> = emptyList(),
    val failure: List<String> = emptyList(),
    val not_executed: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val output_contains: List<String> = emptyList(),
    val output_matches: List<String> = emptyList(),
    val output_doesnt_contain: List<String> = emptyList(),
)

enum class Permission(private val hasPermission: File.() -> Boolean) {
    R(File::canRead), W(File::canWrite), X(File::canExecute);

    fun requireOnFile(file: File) = require(file.hasPermission()) {
        "File ${file.absolutePath} must have permission $name, but it does not."
    }
}

data class ExistingFile(
    val name: String,
    val findRegex: List<String> = emptyList(),
    val contents: List<String> = emptyList(),
    val trim: Boolean = false,
    val permissions: List<Permission> = emptyList(),
) {
    private fun Sequence<String>.trimLinesIfNecessary(): String =
        if (trim) { map(String::trim) } else { this }.joinToString("\n")

    private val File.text: String
        get() = bufferedReader().use {
            it.lines().asSequence().trimLinesIfNecessary()
        }

    private val actualContents: List<String> by lazy {
        contents.map {
            it.lineSequence().trimLinesIfNecessary()
        }
    }

    fun validate(actualFile: File): Unit = with(actualFile) {
        require(exists()) {
            "File $name does not exist."
        }
        if (actualContents.isNotEmpty()) {
            val text = text
            for (content in actualContents) {
                require(content in text) {
                    "Content of $this does not match contain the expected content:\n\t" +
                        content.lines().joinToString("\n\t")
                }
            }
        }
        findRegex.forEach { regexString ->
            val regex = Regex(regexString)
            requireNotNull(readLines().find { regex.matches(it) }) {
                "None of the lines in $this matches the regular expression $findRegex"
            }
        }
        permissions.forEach { it.requireOnFile(this) }
    }
}
