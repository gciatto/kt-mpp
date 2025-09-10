package io.github.gciatto.kt.mpp.test

import io.github.gciatto.kt.mpp.utils.NodeVersions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TestNodeVersions :
    FunSpec({

        test("Retrieving latest Node version works") {
            NodeVersions.latest().shouldBeVersionNumber()
        }

        for (i in 10..23) {
            val version = NodeVersions.latest("$i")
            test("Retrieving latest Node version for major $i returns $version") {
                version.shouldBeVersionNumber()
            }

            val references =
                buildSet {
                    add("latest-$i")
                    add("$i-latest")
                    add("$i.x")
                    add(version)
                    val majorMinor = version.split(".").subList(0, 2).joinToString(".")
                    add(majorMinor)
                    add("$majorMinor.x")
                }

            for (ref in references) {
                test("Retrieving Node version $ref returns $version") {
                    NodeVersions.latest(ref) shouldBe version
                }
            }
        }
    })
