package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NumericTests {
    private val jsonLogic = JsonLogic()

    // ==================== > (greater than) tests ====================

    @Test
    fun `greater than - true case`() {
        assertEquals(true, jsonLogic.apply("""{">":[2, 1]}""", null))
    }

    @Test
    fun `greater than - false case`() {
        assertEquals(false, jsonLogic.apply("""{">":[1, 2]}""", null))
    }

    @Test
    fun `greater than - equal values`() {
        assertEquals(false, jsonLogic.apply("""{">":[1, 1]}""", null))
    }

    // ==================== >= (greater than or equal) tests ====================

    @Test
    fun `greater than or equal - true case`() {
        assertEquals(true, jsonLogic.apply("""{">=":[2, 1]}""", null))
    }

    @Test
    fun `greater than or equal - equal values`() {
        assertEquals(true, jsonLogic.apply("""{">=":[1, 1]}""", null))
    }

    @Test
    fun `greater than or equal - false case`() {
        assertEquals(false, jsonLogic.apply("""{">=":[1, 2]}""", null))
    }

    // ==================== < (less than) tests ====================

    @Test
    fun `less than - true case`() {
        assertEquals(true, jsonLogic.apply("""{"<":[1, 2]}""", null))
    }

    @Test
    fun `less than - false case`() {
        assertEquals(false, jsonLogic.apply("""{"<":[2, 1]}""", null))
    }

    @Test
    fun `less than - between test (exclusive)`() {
        assertEquals(true, jsonLogic.apply("""{"<":[1, 2, 3]}""", null))
        assertEquals(false, jsonLogic.apply("""{"<":[1, 1, 3]}""", null))
        assertEquals(false, jsonLogic.apply("""{"<":[1, 3, 3]}""", null))
    }

    // ==================== <= (less than or equal) tests ====================

    @Test
    fun `less than or equal - true case`() {
        assertEquals(true, jsonLogic.apply("""{"<=":[1, 2]}""", null))
    }

    @Test
    fun `less than or equal - equal values`() {
        assertEquals(true, jsonLogic.apply("""{"<=":[1, 1]}""", null))
    }

    @Test
    fun `less than or equal - between test (inclusive)`() {
        assertEquals(true, jsonLogic.apply("""{"<=":[1, 1, 3]}""", null))
        assertEquals(true, jsonLogic.apply("""{"<=":[1, 3, 3]}""", null))
        assertEquals(false, jsonLogic.apply("""{"<=":[1, 4, 3]}""", null))
    }

    // ==================== max tests ====================

    @Test
    fun `max - finds maximum`() {
        assertEquals(3, jsonLogic.apply("""{"max":[1, 2, 3]}""", null))
    }

    @Test
    fun `max - negative numbers`() {
        assertEquals(-1, jsonLogic.apply("""{"max":[-3, -2, -1]}""", null))
    }

    @Test
    fun `max - single value`() {
        assertEquals(5, jsonLogic.apply("""{"max":[5]}""", null))
    }

    // ==================== min tests ====================

    @Test
    fun `min - finds minimum`() {
        assertEquals(1, jsonLogic.apply("""{"min":[1, 2, 3]}""", null))
    }

    @Test
    fun `min - negative numbers`() {
        assertEquals(-3, jsonLogic.apply("""{"min":[-3, -2, -1]}""", null))
    }

    // ==================== + (addition) tests ====================

    @Test
    fun `add - two numbers`() {
        assertEquals(3, jsonLogic.apply("""{"+":[1, 2]}""", null))
    }

    @Test
    fun `add - multiple numbers`() {
        assertEquals(10, jsonLogic.apply("""{"+":[1, 2, 3, 4]}""", null))
    }

    @Test
    fun `add - single value casts to number`() {
        assertEquals(5, jsonLogic.apply("""{"+":[5]}""", null))
    }

    @Test
    fun `add - string to number conversion`() {
        assertEquals(3, jsonLogic.apply("""{"+":["1", 2]}""", null))
    }

    // ==================== - (subtraction) tests ====================

    @Test
    fun `subtract - two numbers`() {
        assertEquals(1, jsonLogic.apply("""{"-":[3, 2]}""", null))
    }

    @Test
    fun `subtract - single value negates`() {
        assertEquals(-5, jsonLogic.apply("""{"-":[5]}""", null))
    }

    // ==================== * (multiplication) tests ====================

    @Test
    fun `multiply - two numbers`() {
        assertEquals(6, jsonLogic.apply("""{"*":[2, 3]}""", null))
    }

    @Test
    fun `multiply - multiple numbers`() {
        assertEquals(24, jsonLogic.apply("""{"*":[2, 3, 4]}""", null))
    }

    // ==================== / (division) tests ====================

    @Test
    fun `divide - two numbers`() {
        assertEquals(2, jsonLogic.apply("""{"/":[6, 3]}""", null))
    }

    @Test
    fun `divide - with decimals`() {
        assertEquals(2.5, jsonLogic.apply("""{"/":[5, 2]}""", null))
    }

    // ==================== % (modulo) tests ====================

    @Test
    fun `modulo - remainder`() {
        assertEquals(1, jsonLogic.apply("""{"%":[5, 2]}""", null))
    }

    @Test
    fun `modulo - no remainder`() {
        assertEquals(0, jsonLogic.apply("""{"%":[6, 3]}""", null))
    }

    @Test
    fun `modulo - example from docs`() {
        assertEquals(1, jsonLogic.apply("""{"%":[101, 2]}""", null))
    }
}
