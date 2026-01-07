package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for scope climbing functionality with val operator.
 */
class ScopeClimbingTest {
    private val jsonLogic = JsonLogic()

    @Test
    fun `map should provide index via val scope climbing`() {
        // Test: {"map":[[1,2,3],{"val":[[1],"index"]}]}
        // Expected: [0, 1, 2] (index at each iteration)
        val result = jsonLogic.applyJson(
            """{"map":[[1,2,3],{"val":[[1],"index"]}]}""",
            "{}"
        )
        assertEquals(listOf(0, 1, 2), result)
    }

    @Test
    fun `map should provide element via val empty`() {
        // Test: {"map":[[1,2,3],{"val":[]}]}
        // Expected: [1, 2, 3] (element at each iteration)
        val result = jsonLogic.applyJson(
            """{"map":[[1,2,3],{"val":[]}]}""",
            "{}"
        )
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `map should access parent context via val scope climbing`() {
        // Test: {"map":[{"val":"numbers"},{"+": [{"val":[[2],"value"]},{"val":[]}]}]}
        // Data: {"numbers":[1,2,3],"value":10}
        // Expected: [11,12,13] (10+1, 10+2, 10+3)
        val result = jsonLogic.applyJson(
            """{"map":[{"val":"numbers"},{"+": [{"val":[[2],"value"]},{"val":[]}]}]}""",
            """{"numbers":[1,2,3],"value":10}"""
        )
        assertEquals(listOf(11, 12, 13), result)
    }

    @Test
    fun `simple map add index test`() {
        // Test: {"map":[{"val":"numbers"},{"+": [{"val":[[1],"index"]},{"val":[]}]}]}
        // Data: {"numbers":[1,2,3]}
        // Expected: [1,3,5] (1+0, 2+1, 3+2)
        val result = jsonLogic.applyJson(
            """{"map":[{"val":"numbers"},{"+": [{"val":[[1],"index"]},{"val":[]}]}]}""",
            """{"numbers":[1,2,3]}"""
        )
        assertEquals(listOf(1, 3, 5), result)
    }

    @Test
    fun `try with fallback should access parent scope via val`() {
        // Test: {"try":[{"throw":"error"},{"val":[[2],"fallback"]}]}
        // Data: {"fallback":"Hello"}
        // Expected: "Hello"
        // In try fallback:
        // - level 1 = error context {type=error}
        // - level 2 = original data {fallback=Hello}
        val result = jsonLogic.applyJson(
            """{"try":[{"throw":"error"},{"val":[[2],"fallback"]}]}""",
            """{"fallback":"Hello"}"""
        )
        assertEquals("Hello", result)
    }

    @Test
    fun `try with NaN in map should access parent scope in fallback`() {
        // {"try":[{"if":[true,{"map":[[1,2,3],{"/": [0, 0]}]},null]},{"val":[[2],"fallback"]}]}
        // Data: {"fallback":"Hello"}
        // Expected: "Hello"
        val result = jsonLogic.applyJson(
            """{"try":[{"if":[true,{"map":[[1,2,3],{"/": [0, 0]}]},null]},{"val":[[2],"fallback"]}]}""",
            """{"fallback":"Hello"}"""
        )
        assertEquals("Hello", result)
    }
}
