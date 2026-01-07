package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LogicTests {
    private val jsonLogic = JsonLogic()

    // ==================== if tests ====================

    @Test
    fun `if - simple true condition`() {
        assertEquals("yes", jsonLogic.apply("""{"if": [true, "yes", "no"]}""", null))
    }

    @Test
    fun `if - simple false condition`() {
        assertEquals("no", jsonLogic.apply("""{"if": [false, "yes", "no"]}""", null))
    }

    @Test
    fun `if - multiple conditions`() {
        assertEquals(
            "B",
            jsonLogic.apply(
                """{"if": [false, "A", true, "B", "C"]}""",
                null
            )
        )
    }

    @Test
    fun `if - falls through to default`() {
        assertEquals(
            "default",
            jsonLogic.apply(
                """{"if": [false, "A", false, "B", "default"]}""",
                null
            )
        )
    }

    @Test
    fun `if - with data`() {
        assertEquals(
            "hot",
            jsonLogic.apply(
                """{"if": [{"<": [{"var": "temp"}, 0]}, "freezing", {"<": [{"var": "temp"}, 20]}, "cold", "hot"]}""",
                mapOf("temp" to 30)
            )
        )
    }

    // ==================== == (equality) tests ====================

    @Test
    fun `equality - same numbers`() {
        assertEquals(true, jsonLogic.apply("""{"==": [1, 1]}""", null))
    }

    @Test
    fun `equality - different numbers`() {
        assertEquals(false, jsonLogic.apply("""{"==": [1, 2]}""", null))
    }

    @Test
    fun `equality - string and number coercion`() {
        assertEquals(true, jsonLogic.apply("""{"==": ["1", 1]}""", null))
    }

    @Test
    fun `equality - same strings`() {
        assertEquals(true, jsonLogic.apply("""{"==": ["hello", "hello"]}""", null))
    }

    // ==================== === (strict equality) tests ====================

    @Test
    fun `strict equality - same type same value`() {
        assertEquals(true, jsonLogic.apply("""{"===": [1, 1]}""", null))
    }

    @Test
    fun `strict equality - different types`() {
        assertEquals(false, jsonLogic.apply("""{"===": ["1", 1]}""", null))
    }

    // ==================== != (inequality) tests ====================

    @Test
    fun `inequality - different values`() {
        assertEquals(true, jsonLogic.apply("""{"!=": [1, 2]}""", null))
    }

    @Test
    fun `inequality - same values`() {
        assertEquals(false, jsonLogic.apply("""{"!=": [1, 1]}""", null))
    }

    // ==================== !== (strict inequality) tests ====================

    @Test
    fun `strict inequality - different types`() {
        assertEquals(true, jsonLogic.apply("""{"!==": ["1", 1]}""", null))
    }

    @Test
    fun `strict inequality - same type same value`() {
        assertEquals(false, jsonLogic.apply("""{"!==": [1, 1]}""", null))
    }

    // ==================== ! (not) tests ====================

    @Test
    fun `not - true becomes false`() {
        assertEquals(false, jsonLogic.apply("""{"!": true}""", null))
    }

    @Test
    fun `not - false becomes true`() {
        assertEquals(true, jsonLogic.apply("""{"!": false}""", null))
    }

    @Test
    fun `not - truthy value becomes false`() {
        assertEquals(false, jsonLogic.apply("""{"!": 1}""", null))
    }

    @Test
    fun `not - falsy value becomes true`() {
        assertEquals(true, jsonLogic.apply("""{"!": 0}""", null))
    }

    @Test
    fun `not - empty array is falsy`() {
        assertEquals(true, jsonLogic.apply("""{"!": []}""", null))
    }

    // ==================== !! (double negation) tests ====================

    @Test
    fun `double negation - truthy stays true`() {
        assertEquals(true, jsonLogic.apply("""{"!!": [1]}""", null))
    }

    @Test
    fun `double negation - falsy stays false`() {
        assertEquals(false, jsonLogic.apply("""{"!!": [0]}""", null))
    }

    @Test
    fun `double negation - empty array is falsy`() {
        assertEquals(false, jsonLogic.apply("""{"!!": [[]]}""", null))
    }

    // ==================== or tests ====================

    @Test
    fun `or - returns first truthy`() {
        assertEquals(1, jsonLogic.apply("""{"or": [false, 1, 2]}""", null))
    }

    @Test
    fun `or - returns last if all falsy`() {
        assertEquals(0, jsonLogic.apply("""{"or": [false, 0]}""", null))
    }

    @Test
    fun `or - short circuits`() {
        assertEquals(true, jsonLogic.apply("""{"or": [true, {"var": "undefined"}]}""", null))
    }

    // ==================== and tests ====================

    @Test
    fun `and - returns first falsy`() {
        assertEquals(false, jsonLogic.apply("""{"and": [true, false, 1]}""", null))
    }

    @Test
    fun `and - returns last if all truthy`() {
        assertEquals(3, jsonLogic.apply("""{"and": [1, 2, 3]}""", null))
    }

    @Test
    fun `and - short circuits`() {
        assertEquals(false, jsonLogic.apply("""{"and": [false, {"var": "undefined"}]}""", null))
    }
}
