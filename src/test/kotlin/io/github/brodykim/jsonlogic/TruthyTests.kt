package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TruthyTests {

    @Test
    fun `truthy - null is falsy`() {
        assertFalse(JsonLogic.truthy(null))
    }

    @Test
    fun `truthy - false is falsy`() {
        assertFalse(JsonLogic.truthy(false))
    }

    @Test
    fun `truthy - true is truthy`() {
        assertTrue(JsonLogic.truthy(true))
    }

    @Test
    fun `truthy - zero is falsy`() {
        assertFalse(JsonLogic.truthy(0))
        assertFalse(JsonLogic.truthy(0.0))
        assertFalse(JsonLogic.truthy(0L))
    }

    @Test
    fun `truthy - non-zero numbers are truthy`() {
        assertTrue(JsonLogic.truthy(1))
        assertTrue(JsonLogic.truthy(-1))
        assertTrue(JsonLogic.truthy(0.1))
        assertTrue(JsonLogic.truthy(100L))
    }

    @Test
    fun `truthy - NaN is falsy`() {
        assertFalse(JsonLogic.truthy(Double.NaN))
    }

    @Test
    fun `truthy - empty string is falsy`() {
        assertFalse(JsonLogic.truthy(""))
    }

    @Test
    fun `truthy - non-empty string is truthy`() {
        assertTrue(JsonLogic.truthy("hello"))
        assertTrue(JsonLogic.truthy(" "))
        assertTrue(JsonLogic.truthy("0"))
        assertTrue(JsonLogic.truthy("false"))
    }

    @Test
    fun `truthy - empty list is falsy`() {
        assertFalse(JsonLogic.truthy(emptyList<Any>()))
    }

    @Test
    fun `truthy - non-empty list is truthy`() {
        assertTrue(JsonLogic.truthy(listOf(1)))
        assertTrue(JsonLogic.truthy(listOf(false)))
        assertTrue(JsonLogic.truthy(listOf(null)))
    }

    @Test
    fun `truthy - empty array is falsy`() {
        assertFalse(JsonLogic.truthy(emptyArray<Any>()))
    }

    @Test
    fun `truthy - non-empty array is truthy`() {
        assertTrue(JsonLogic.truthy(arrayOf(1)))
    }

    @Test
    fun `truthy - objects are truthy`() {
        assertTrue(JsonLogic.truthy(mapOf<String, Any>()))
        assertTrue(JsonLogic.truthy(mapOf("a" to 1)))
        assertTrue(JsonLogic.truthy(Any()))
    }
}
