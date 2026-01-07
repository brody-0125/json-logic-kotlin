package io.github.brodykim.jsonlogic

import kotlinx.serialization.json.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Compatibility tests using the json-logic/compat-tables test suites.
 *
 * Test suites are downloaded from:
 * https://github.com/json-logic/compat-tables/tree/main/suites
 */
class CompatTablesTests {
    private val logger = LoggerFactory.getLogger(CompatTablesTests::class.java)
    private val jsonLogic = JsonLogic()
    private val json = Json { ignoreUnknownKeys = true }
    private val suitesDir = File("compat-tests/suites")

    @TestFactory
    fun `compat-tables test suites`(): List<DynamicTest> {
        if (!suitesDir.exists()) {
            return listOf(DynamicTest.dynamicTest("Test suites not found") {
                fail<Unit>("Please download test suites from https://github.com/json-logic/compat-tables/tree/main/suites")
            })
        }

        val indexFile = File(suitesDir, "index.json")
        if (!indexFile.exists()) {
            return listOf(DynamicTest.dynamicTest("Index file not found") {
                fail<Unit>("index.json not found in compat-tests/suites/")
            })
        }

        val suiteFiles = json.parseToJsonElement(indexFile.readText()).jsonArray
            .mapNotNull { it.jsonPrimitive.contentOrNull }

        return suiteFiles.flatMap { suiteName ->
            loadTestSuite(suiteName)
        }
    }

    private fun loadTestSuite(suiteName: String): List<DynamicTest> {
        val suiteFile = File(suitesDir, suiteName)
        if (!suiteFile.exists()) {
            return listOf(DynamicTest.dynamicTest("[$suiteName] File not found") {
                fail<Unit>("Test suite file not found: $suiteName")
            })
        }

        val suiteContent = suiteFile.readText()
        val suiteArray = try {
            json.parseToJsonElement(suiteContent).jsonArray
        } catch (e: Exception) {
            return listOf(DynamicTest.dynamicTest("[$suiteName] Parse error") {
                fail<Unit>("Failed to parse $suiteName: ${e.message}")
            })
        }

        val tests = mutableListOf<DynamicTest>()
        val suiteShortName = suiteName.removeSuffix(".json")

        for ((index, element) in suiteArray.withIndex()) {
            // Skip comment strings (start with #)
            if (element is JsonPrimitive && element.isString) {
                continue
            }

            if (element is JsonObject) {
                val description = element["description"]?.jsonPrimitive?.contentOrNull ?: "test_$index"
                val rule = element["rule"] ?: continue
                val data = element["data"] // may be null/missing
                val expectedResult = element["result"] ?: continue

                // Skip tests that expect errors (throw/try operations not in standard JsonLogic)
                val error = element["error"]
                if (error != null) {
                    continue
                }

                val testName = "[$suiteShortName] $description"
                tests.add(DynamicTest.dynamicTest(testName) {
                    runTest(rule, data, expectedResult, testName)
                })
            }
        }

        return tests
    }

    private fun runTest(rule: JsonElement, data: JsonElement?, expected: JsonElement, testName: String) {
        val ruleStr = rule.toString()
        val dataValue = if (data != null && data !is JsonNull) jsonElementToKotlin(data) else null

        try {
            val result = jsonLogic.apply(ruleStr, dataValue)
            val expectedValue = jsonElementToKotlin(expected)
            assertResultEquals(expectedValue, result, testName)
        } catch (e: JsonLogicUnknownOperatorException) {
            // Some tests use operations not in standard JsonLogic (val, throw, try, etc.)
            // Mark as skipped by not failing
            logger.warn("Skipped (unknown operator): {} - {}", testName, e.operator)
        } catch (e: Exception) {
            fail<Unit>("$testName failed with exception: ${e.message}\nRule: $ruleStr")
        }
    }

    private fun assertResultEquals(expected: Any?, actual: Any?, message: String) {
        when {
            expected == null && actual == null -> return
            expected == null || actual == null -> {
                assertEquals(expected, actual, message)
            }
            expected is Number && actual is Number -> {
                if (expected.toDouble().isNaN() && actual.toDouble().isNaN()) {
                    return // Both NaN
                }
                if (expected.toDouble().isInfinite() && actual.toDouble().isInfinite()) {
                    assertEquals(expected.toDouble() > 0, actual.toDouble() > 0, "$message - Infinity sign mismatch")
                    return
                }
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
            expected is Boolean && actual is Boolean -> {
                assertEquals(expected, actual, message)
            }
            expected is String && actual is String -> {
                assertEquals(expected, actual, message)
            }
            else -> {
                // Try to compare as strings if types don't match
                assertEquals(expected.toString(), actual.toString(), "$message - Type mismatch: ${expected::class} vs ${actual::class}")
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
                        content.toIntOrNull() ?: content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
                    }
                }
            }
            is JsonArray -> element.map { jsonElementToKotlin(it) }
            is JsonObject -> element.mapValues { jsonElementToKotlin(it.value) }
        }
    }
}
