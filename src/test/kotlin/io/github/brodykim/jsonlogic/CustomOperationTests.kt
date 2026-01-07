package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CustomOperationTests {

    @Test
    fun `custom operation - simple addition`() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("plus") { args, _, _ ->
            val a = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            val b = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
            (a + b).toInt()
        }

        assertEquals(42, jsonLogic.apply("""{"plus": [23, 19]}""", null))
    }

    @Test
    fun `custom operation - string repeat`() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("repeat") { args, _, _ ->
            val str = args.getOrNull(0)?.toString() ?: ""
            val times = (args.getOrNull(1) as? Number)?.toInt() ?: 1
            str.repeat(times)
        }

        assertEquals("abcabcabc", jsonLogic.apply("""{"repeat": ["abc", 3]}""", null))
    }

    @Test
    fun `custom operation - with data access`() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("greet") { args, _, _ ->
            val name = args.getOrNull(0)?.toString() ?: "World"
            "Hello, $name!"
        }

        assertEquals(
            "Hello, John!",
            jsonLogic.apply(
                """{"greet": [{"var": "name"}]}""",
                mapOf("name" to "John")
            )
        )
    }

    @Test
    fun `custom operation - override existing`() {
        val jsonLogic = JsonLogic()
        // Override the + operator to always return 999
        jsonLogic.addOperation("+") { _, _, _ -> 999 }

        assertEquals(999, jsonLogic.apply("""{"+": [1, 2]}""", null))
    }

    @Test
    fun `custom operation - complex logic`() {
        val jsonLogic = JsonLogic()
        jsonLogic.addOperation("between") { args, _, _ ->
            val value = (args.getOrNull(0) as? Number)?.toDouble() ?: return@addOperation false
            val min = (args.getOrNull(1) as? Number)?.toDouble() ?: return@addOperation false
            val max = (args.getOrNull(2) as? Number)?.toDouble() ?: return@addOperation false
            value in min..max
        }

        assertEquals(true, jsonLogic.apply("""{"between": [5, 1, 10]}""", null))
        assertEquals(false, jsonLogic.apply("""{"between": [15, 1, 10]}""", null))
    }
}
