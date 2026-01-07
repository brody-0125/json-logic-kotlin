package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DataAccessTests {
    private val jsonLogic = JsonLogic()

    // ==================== var tests ====================

    @Test
    fun `var - simple property access`() {
        assertEquals("bar", jsonLogic.apply("""{"var": "foo"}""", mapOf("foo" to "bar")))
    }

    @Test
    fun `var - nested property access with dot notation`() {
        assertEquals(
            "Ringo",
            jsonLogic.apply(
                """{"var": "champ.name"}""",
                mapOf("champ" to mapOf("name" to "Ringo"))
            )
        )
    }

    @Test
    fun `var - array index access`() {
        assertEquals(2, jsonLogic.apply("""{"var": 1}""", listOf(1, 2, 3)))
    }

    @Test
    fun `var - empty string returns entire data`() {
        val data = mapOf("a" to 1, "b" to 2)
        assertEquals(data, jsonLogic.apply("""{"var": ""}""", data))
    }

    @Test
    fun `var - default value when missing`() {
        assertEquals(26, jsonLogic.apply("""{"var": ["z", 26]}""", mapOf("a" to 1)))
    }

    @Test
    fun `var - null data returns default`() {
        assertEquals("default", jsonLogic.apply("""{"var": ["x", "default"]}""", null))
    }

    @Test
    fun `var - deep nested access`() {
        assertEquals(
            "value",
            jsonLogic.apply(
                """{"var": "a.b.c"}""",
                mapOf("a" to mapOf("b" to mapOf("c" to "value")))
            )
        )
    }

    @Test
    fun `var - array access in nested object`() {
        assertEquals(
            "second",
            jsonLogic.apply(
                """{"var": "items.1"}""",
                mapOf("items" to listOf("first", "second", "third"))
            )
        )
    }

    // ==================== missing tests ====================

    @Test
    fun `missing - returns missing keys`() {
        assertEquals(
            listOf("b"),
            jsonLogic.apply("""{"missing": ["a", "b"]}""", mapOf("a" to 1))
        )
    }

    @Test
    fun `missing - returns empty when all present`() {
        assertEquals(
            emptyList<String>(),
            jsonLogic.apply("""{"missing": ["a", "b"]}""", mapOf("a" to 1, "b" to 2))
        )
    }

    @Test
    fun `missing - empty string is considered missing`() {
        assertEquals(
            listOf("a"),
            jsonLogic.apply("""{"missing": ["a"]}""", mapOf("a" to ""))
        )
    }

    @Test
    fun `missing - null data returns all keys as missing`() {
        assertEquals(
            listOf("a", "b"),
            jsonLogic.apply("""{"missing": ["a", "b"]}""", null)
        )
    }

    // ==================== missing_some tests ====================

    @Test
    fun `missing_some - returns empty when minimum met`() {
        assertEquals(
            emptyList<String>(),
            jsonLogic.apply(
                """{"missing_some": [1, ["a", "b", "c"]]}""",
                mapOf("a" to 1)
            )
        )
    }

    @Test
    fun `missing_some - returns missing when minimum not met`() {
        assertEquals(
            listOf("a", "b", "c"),
            jsonLogic.apply(
                """{"missing_some": [2, ["a", "b", "c"]]}""",
                mapOf("d" to 1)
            )
        )
    }

    @Test
    fun `missing_some - all present returns empty`() {
        assertEquals(
            emptyList<String>(),
            jsonLogic.apply(
                """{"missing_some": [2, ["a", "b"]]}""",
                mapOf("a" to 1, "b" to 2)
            )
        )
    }
}
