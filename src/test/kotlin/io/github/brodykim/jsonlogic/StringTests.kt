package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StringTests {
    private val jsonLogic = JsonLogic()

    // ==================== cat tests ====================

    @Test
    fun `cat - simple concatenation`() {
        assertEquals(
            "Hello World",
            jsonLogic.apply("""{"cat":["Hello", " ", "World"]}""", null)
        )
    }

    @Test
    fun `cat - with numbers`() {
        assertEquals(
            "The answer is 42",
            jsonLogic.apply("""{"cat":["The answer is ", 42]}""", null)
        )
    }

    @Test
    fun `cat - empty`() {
        assertEquals(
            "",
            jsonLogic.apply("""{"cat":[]}""", null)
        )
    }

    @Test
    fun `cat - single string`() {
        assertEquals(
            "hello",
            jsonLogic.apply("""{"cat":["hello"]}""", null)
        )
    }

    @Test
    fun `cat - with data`() {
        assertEquals(
            "Hello John!",
            jsonLogic.apply(
                """{"cat":["Hello ", {"var":"name"}, "!"]}""",
                mapOf("name" to "John")
            )
        )
    }

    // ==================== substr tests ====================

    @Test
    fun `substr - from start position`() {
        assertEquals(
            "logic",
            jsonLogic.apply("""{"substr":["jsonlogic", 4]}""", null)
        )
    }

    @Test
    fun `substr - negative start (from end)`() {
        assertEquals(
            "logic",
            jsonLogic.apply("""{"substr":["jsonlogic", -5]}""", null)
        )
    }

    @Test
    fun `substr - with length`() {
        assertEquals(
            "json",
            jsonLogic.apply("""{"substr":["jsonlogic", 0, 4]}""", null)
        )
    }

    @Test
    fun `substr - negative start with length`() {
        assertEquals(
            "log",
            jsonLogic.apply("""{"substr":["jsonlogic", -5, 3]}""", null)
        )
    }

    @Test
    fun `substr - negative length (stop before end)`() {
        assertEquals(
            "log",
            jsonLogic.apply("""{"substr":["jsonlogic", 4, -2]}""", null)
        )
    }

    @Test
    fun `substr - empty string`() {
        assertEquals(
            "",
            jsonLogic.apply("""{"substr":["", 0]}""", null)
        )
    }

    @Test
    fun `substr - start beyond string length`() {
        assertEquals(
            "",
            jsonLogic.apply("""{"substr":["abc", 10]}""", null)
        )
    }
}
