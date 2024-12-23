package io.github.gciatto.kt.mpp.test

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.classgraph.ClassGraph
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class Tests : StringSpec(
    {
        val scan = ClassGraph()
            .enableAllInfo()
            .acceptPackages(Tests::class.java.`package`.name)
            .scan()
        scan.getResourcesWithLeafName("test.yaml")
            .flatMap {
                log.debug("Found test list in {}", it)
                val yamlFile = File(it.classpathElementFile.absolutePath + "/" + it.path)
                val testConfiguration = Config {
                    addSpec(Root)
                }.from.yaml.inputStream(it.open())
                val sharedOptions = testConfiguration[Root.options]
                testConfiguration[Root.tests].map { test -> Triple(test, yamlFile.parentFile, sharedOptions) }
            }
            .forEach { (test, location, options) ->
                log.debug("Test to be executed: {} from {}", test, location)
                val testFolder = folder {
                    location.copyRecursively(this.root)
                }
                log.debug("Test has been copied into {} and is ready to get executed", testFolder)
                test.description {
                    val result = GradleRunner.create()
                        .withProjectDir(testFolder.root)
                        .withPluginClasspath()
                        .withArguments(test.configuration.tasks + options + test.configuration.options)
//                        .withDebug(true)
                        .run { if (test.expectation.failure.isEmpty()) build() else buildAndFail() }
                    println(result.tasks)
                    println(result.output)
                    test.expectation.output_contains.forEach {
                        result.output shouldContain it
                    }
                    test.expectation.output_doesnt_contain.forEach {
                        result.output shouldNotContain it
                    }
                    test.expectation.output_matches.forEach {
                        result.output should MatchMultilinePattern(it)
                    }
                    test.expectation.success.forEach {
                        result.outcomeOf(it) shouldBeIn listOf(
                            TaskOutcome.SUCCESS,
                            TaskOutcome.UP_TO_DATE,
                            TaskOutcome.NO_SOURCE,
                        )
                    }
                    test.expectation.failure.forEach {
                        result.outcomeOf(it) shouldBe TaskOutcome.FAILED
                    }
                    test.expectation.skipped.forEach {
                        result.outcomeOf(it) shouldBe TaskOutcome.SKIPPED
                    }
                    test.expectation.not_executed.forEach {
                        result.task(it) shouldBe null
                    }
                    test.expectation.file_exists.forEach {
                        val file = File("${testFolder.root.absolutePath}/${it.name}").apply {
                            shouldExist()
                            shouldBeAFile()
                        }
                        it.validate(file)
                    }
                }
            }
    },
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Tests::class.java)

        private fun BuildResult.outcomeOf(path: String) = checkNotNull(task(path)?.outcome) {
            "Task $path was not present among the executed tasks"
        }

        private fun folder(closure: TemporaryFolder.() -> Unit) = TemporaryFolder().apply {
            create()
            closure()
        }
    }
}
