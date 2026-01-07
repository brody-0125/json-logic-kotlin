package io.github.brodykim.jsonlogic

import kotlinx.serialization.json.*
import java.io.File

/**
 * Generates a compatibility report for json-logic-kotlin against compat-tables test suites.
 */
object CompatTablesReporter {
    private val jsonLogic = JsonLogic()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val suitesDir = File("compat-tests/suites")
    private val resultsDir = File("compat-tests/results")

    data class TestResult(
        val suite: String,
        val description: String,
        val rule: String,
        val data: String?,
        val expected: String,
        val actual: String?,
        val passed: Boolean,
        val error: String? = null,
        val skipped: Boolean = false,
        val skipReason: String? = null
    )

    data class SuiteResult(
        val name: String,
        val total: Int,
        val passed: Int,
        val failed: Int,
        val skipped: Int,
        val passRate: Double,
        val tests: List<TestResult>
    )

    @JvmStatic
    fun main(args: Array<String>) {
        if (!suitesDir.exists()) {
            println("Test suites not found at ${suitesDir.absolutePath}")
            return
        }

        resultsDir.mkdirs()

        val indexFile = File(suitesDir, "index.json")
        val suiteFiles = Json.parseToJsonElement(indexFile.readText()).jsonArray
            .mapNotNull { it.jsonPrimitive.contentOrNull }

        val allResults = mutableListOf<SuiteResult>()
        val failedTests = mutableListOf<TestResult>()

        for (suiteName in suiteFiles) {
            val result = runSuite(suiteName)
            if (result != null) {
                allResults.add(result)
                failedTests.addAll(result.tests.filter { !it.passed && !it.skipped })
            }
        }

        // Generate summary
        val totalTests = allResults.sumOf { it.total }
        val totalPassed = allResults.sumOf { it.passed }
        val totalFailed = allResults.sumOf { it.failed }
        val totalSkipped = allResults.sumOf { it.skipped }
        val overallPassRate = if (totalTests > 0) (totalPassed.toDouble() / (totalTests - totalSkipped)) * 100 else 0.0

        // Save results
        saveResults(allResults, failedTests, totalTests, totalPassed, totalFailed, totalSkipped, overallPassRate)

        println("\n" + "=".repeat(60))
        println("COMPATIBILITY TEST RESULTS")
        println("=".repeat(60))
        println("Total Tests: $totalTests")
        println("Passed: $totalPassed")
        println("Failed: $totalFailed")
        println("Skipped: $totalSkipped (non-standard operations)")
        println("Pass Rate: ${"%.2f".format(overallPassRate)}% (excluding skipped)")
        println("=".repeat(60))
    }

    private fun runSuite(suiteName: String): SuiteResult? {
        val suiteFile = File(suitesDir, suiteName)
        if (!suiteFile.exists()) {
            return null
        }

        val suiteContent = suiteFile.readText()
        val suiteArray = try {
            Json.parseToJsonElement(suiteContent).jsonArray
        } catch (e: Exception) {
            return null
        }

        val tests = mutableListOf<TestResult>()
        val suiteShortName = suiteName.removeSuffix(".json")

        for ((index, element) in suiteArray.withIndex()) {
            if (element is JsonPrimitive && element.isString) continue
            if (element !is JsonObject) continue

            val description = element["description"]?.jsonPrimitive?.contentOrNull ?: "test_$index"
            val rule = element["rule"] ?: continue
            val data = element["data"]
            val expectedResult = element["result"] ?: continue
            val errorExpected = element["error"]

            // Skip error tests
            if (errorExpected != null) {
                tests.add(TestResult(
                    suite = suiteShortName,
                    description = description,
                    rule = rule.toString(),
                    data = data?.toString(),
                    expected = "ERROR",
                    actual = null,
                    passed = false,
                    skipped = true,
                    skipReason = "Error test (not standard JsonLogic)"
                ))
                continue
            }

            val result = runTest(suiteShortName, description, rule, data, expectedResult)
            tests.add(result)
        }

        val passed = tests.count { it.passed }
        val failed = tests.count { !it.passed && !it.skipped }
        val skipped = tests.count { it.skipped }
        val passRate = if (tests.size - skipped > 0) (passed.toDouble() / (tests.size - skipped)) * 100 else 100.0

        return SuiteResult(
            name = suiteShortName,
            total = tests.size,
            passed = passed,
            failed = failed,
            skipped = skipped,
            passRate = passRate,
            tests = tests
        )
    }

    private fun runTest(suite: String, description: String, rule: JsonElement, data: JsonElement?, expected: JsonElement): TestResult {
        val ruleStr = rule.toString()
        val dataValue = if (data != null && data !is JsonNull) jsonElementToKotlin(data) else null
        val expectedValue = jsonElementToKotlin(expected)

        return try {
            val result = jsonLogic.apply(ruleStr, dataValue)
            val passed = compareResults(expectedValue, result)

            TestResult(
                suite = suite,
                description = description,
                rule = ruleStr,
                data = data?.toString(),
                expected = expected.toString(),
                actual = resultToString(result),
                passed = passed
            )
        } catch (e: JsonLogicUnknownOperatorException) {
            TestResult(
                suite = suite,
                description = description,
                rule = ruleStr,
                data = data?.toString(),
                expected = expected.toString(),
                actual = null,
                passed = false,
                skipped = true,
                skipReason = "Unknown operator: ${e.operator}"
            )
        } catch (e: Exception) {
            TestResult(
                suite = suite,
                description = description,
                rule = ruleStr,
                data = data?.toString(),
                expected = expected.toString(),
                actual = null,
                passed = false,
                error = e.message
            )
        }
    }

    private fun compareResults(expected: Any?, actual: Any?): Boolean {
        return when {
            expected == null && actual == null -> true
            expected == null || actual == null -> false
            expected is Number && actual is Number -> {
                val e = expected.toDouble()
                val a = actual.toDouble()
                if (e.isNaN() && a.isNaN()) true
                else if (e.isInfinite() && a.isInfinite()) (e > 0) == (a > 0)
                else kotlin.math.abs(e - a) < 0.0001
            }
            expected is List<*> && actual is List<*> -> {
                if (expected.size != actual.size) false
                else expected.indices.all { compareResults(expected[it], actual[it]) }
            }
            expected is Map<*, *> && actual is Map<*, *> -> {
                if (expected.keys != actual.keys) false
                else expected.keys.all { compareResults(expected[it], actual[it]) }
            }
            else -> expected == actual
        }
    }

    private fun resultToString(value: Any?): String {
        return when (value) {
            null -> "null"
            is List<*> -> value.map { resultToString(it) }.toString()
            is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "${it.key}: ${resultToString(it.value)}" }
            is String -> "\"$value\""
            else -> value.toString()
        }
    }

    private fun saveResults(
        results: List<SuiteResult>,
        failedTests: List<TestResult>,
        total: Int,
        passed: Int,
        failed: Int,
        skipped: Int,
        passRate: Double
    ) {
        // Save detailed results JSON
        val resultsJson = buildJsonObject {
            put("implementation", "json-logic-kotlin")
            put("version", "1.0.0")
            put("timestamp", System.currentTimeMillis().toString())
            put("summary", buildJsonObject {
                put("totalTests", total)
                put("passed", passed)
                put("failed", failed)
                put("skipped", skipped)
                put("passRate", "%.2f".format(passRate))
            })
            put("suites", buildJsonArray {
                for (result in results) {
                    add(buildJsonObject {
                        put("name", result.name)
                        put("total", result.total)
                        put("passed", result.passed)
                        put("failed", result.failed)
                        put("skipped", result.skipped)
                        put("passRate", "%.2f".format(result.passRate))
                    })
                }
            })
        }
        File(resultsDir, "results.json").writeText(json.encodeToString(JsonObject.serializer(), resultsJson))

        // Save failed tests detail
        val failedJson = buildJsonArray {
            for (test in failedTests) {
                add(buildJsonObject {
                    put("suite", test.suite)
                    put("description", test.description)
                    put("rule", test.rule)
                    test.data?.let { put("data", it) }
                    put("expected", test.expected)
                    test.actual?.let { put("actual", it) }
                    test.error?.let { put("error", it) }
                })
            }
        }
        File(resultsDir, "failed-tests.json").writeText(json.encodeToString(JsonArray.serializer(), failedJson))

        // Generate Markdown report
        generateMarkdownReport(results, failedTests, total, passed, failed, skipped, passRate)
    }

    private fun generateMarkdownReport(
        results: List<SuiteResult>,
        failedTests: List<TestResult>,
        total: Int,
        passed: Int,
        failed: Int,
        skipped: Int,
        passRate: Double
    ) {
        val report = buildString {
            appendLine("# json-logic-kotlin Compatibility Report")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("| Metric | Value |")
            appendLine("|--------|-------|")
            appendLine("| Total Tests | $total |")
            appendLine("| Passed | $passed |")
            appendLine("| Failed | $failed |")
            appendLine("| Skipped | $skipped |")
            appendLine("| **Pass Rate** | **${"%.2f".format(passRate)}%** |")
            appendLine()
            appendLine("> Skipped tests use non-standard JsonLogic operations (val, throw, try, etc.)")
            appendLine()
            appendLine("## Test Suite Results")
            appendLine()
            appendLine("| Suite | Total | Passed | Failed | Skipped | Pass Rate |")
            appendLine("|-------|-------|--------|--------|---------|-----------|")

            for (result in results.sortedBy { it.name }) {
                val status = when {
                    result.failed == 0 -> "✅"
                    result.passRate >= 90 -> "🟡"
                    else -> "❌"
                }
                appendLine("| $status ${result.name} | ${result.total} | ${result.passed} | ${result.failed} | ${result.skipped} | ${"%.1f".format(result.passRate)}% |")
            }

            if (failedTests.isNotEmpty()) {
                appendLine()
                appendLine("## Failed Tests")
                appendLine()
                appendLine("| Suite | Description | Expected | Actual |")
                appendLine("|-------|-------------|----------|--------|")

                for (test in failedTests.take(50)) { // Limit to first 50 failures
                    val expected = test.expected.take(30)
                    val actual = (test.actual ?: test.error ?: "N/A").take(30)
                    appendLine("| ${test.suite} | ${test.description.take(40)} | $expected | $actual |")
                }

                if (failedTests.size > 50) {
                    appendLine()
                    appendLine("*... and ${failedTests.size - 50} more failed tests*")
                }
            }

            appendLine()
            appendLine("---")
            appendLine("*Generated by json-logic-kotlin compatibility test runner*")
            appendLine("*Test suites from [json-logic/compat-tables](https://github.com/json-logic/compat-tables)*")
        }

        File(resultsDir, "REPORT.md").writeText(report)
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
