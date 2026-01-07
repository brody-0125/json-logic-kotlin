package io.github.brodykim.jsonlogic

import kotlinx.serialization.json.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Compatibility tests using the official JsonLogic test suite from jsonlogic.com/tests.json
 */
class CompatibilityTests {
    private val jsonLogic = JsonLogic()
    private val json = Json { ignoreUnknownKeys = true }

    @TestFactory
    fun `official jsonlogic test suite`(): List<DynamicTest> {
        val testsFile = File("src/test/resources/tests.json")
        if (!testsFile.exists()) {
            return listOf(DynamicTest.dynamicTest("Test file not found") {
                fail<Unit>("Please download tests.json from https://jsonlogic.com/tests.json to src/test/resources/")
            })
        }

        val testsJson = testsFile.readText()
        val testsArray = json.parseToJsonElement(testsJson).jsonArray

        val tests = mutableListOf<DynamicTest>()
        var currentComment = ""

        for (element in testsArray) {
            when (element) {
                is JsonPrimitive -> {
                    // Comments are strings
                    if (element.isString) {
                        currentComment = element.content
                    }
                }
                is JsonArray -> {
                    if (element.size >= 3) {
                        val rule = element[0]
                        val data = element[1]
                        val expected = element[2]

                        val testName = buildTestName(currentComment, rule, expected)
                        tests.add(DynamicTest.dynamicTest(testName) {
                            runTest(rule, data, expected)
                        })
                    }
                }
                else -> { /* Skip other elements */ }
            }
        }

        return tests
    }

    private fun buildTestName(comment: String, rule: JsonElement, expected: JsonElement): String {
        val ruleStr = rule.toString().take(50)
        val expectedStr = expected.toString().take(20)
        return "${comment.removePrefix("# ")}: $ruleStr -> $expectedStr"
    }

    private fun runTest(rule: JsonElement, data: JsonElement, expected: JsonElement) {
        val ruleStr = rule.toString()
        val dataValue = jsonElementToKotlin(data)
        val result = jsonLogic.apply(ruleStr, dataValue)
        val expectedValue = jsonElementToKotlin(expected)

        assertResultEquals(expectedValue, result, "Rule: $ruleStr, Data: $data")
    }

    private fun assertResultEquals(expected: Any?, actual: Any?, message: String) {
        when {
            expected == null && actual == null -> return
            expected == null || actual == null -> {
                assertEquals(expected, actual, message)
            }
            expected is Number && actual is Number -> {
                assertEquals(expected.toDouble(), actual.toDouble(), 0.0001, message)
            }
            expected is List<*> && actual is List<*> -> {
                assertEquals(expected.size, actual.size, "$message - List size mismatch")
                for (i in expected.indices) {
                    assertResultEquals(expected[i], actual[i], "$message - Element $i")
                }
            }
            expected is Map<*, *> && actual is Map<*, *> -> {
                assertEquals(expected.keys, actual.keys, "$message - Map keys mismatch")
                for (key in expected.keys) {
                    assertResultEquals(expected[key], actual[key], "$message - Key $key")
                }
            }
            else -> {
                assertEquals(expected, actual, message)
            }
        }
    }

    private fun jsonElementToKotlin(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    else -> {
                        val content = element.content
                        // Try to preserve integer types
                        content.toIntOrNull() ?: content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
                    }
                }
            }
            is JsonArray -> element.map { jsonElementToKotlin(it) }
            is JsonObject -> element.mapValues { jsonElementToKotlin(it.value) }
        }
    }
}
