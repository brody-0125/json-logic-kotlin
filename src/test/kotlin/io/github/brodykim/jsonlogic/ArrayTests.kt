package io.github.brodykim.jsonlogic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ArrayTests {
    private val jsonLogic = JsonLogic()

    // ==================== map tests ====================

    @Test
    fun `map - double each element`() {
        assertEquals(
            listOf(2, 4, 6, 8, 10),
            jsonLogic.apply(
                """{"map":[{"var":"integers"}, {"*":[{"var":""},2]}]}""",
                mapOf("integers" to listOf(1, 2, 3, 4, 5))
            )
        )
    }

    @Test
    fun `map - empty array`() {
        assertEquals(
            emptyList<Any>(),
            jsonLogic.apply(
                """{"map":[{"var":"integers"}, {"*":[{"var":""},2]}]}""",
                mapOf("integers" to emptyList<Int>())
            )
        )
    }

    // ==================== filter tests ====================

    @Test
    fun `filter - filter greater than 2`() {
        assertEquals(
            listOf(3, 4, 5),
            jsonLogic.apply(
                """{"filter":[{"var":"integers"}, {">":[{"var":""},2]}]}""",
                mapOf("integers" to listOf(1, 2, 3, 4, 5))
            )
        )
    }

    @Test
    fun `filter - empty result`() {
        assertEquals(
            emptyList<Any>(),
            jsonLogic.apply(
                """{"filter":[{"var":"integers"}, {">":[{"var":""},10]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    // ==================== reduce tests ====================

    @Test
    fun `reduce - sum`() {
        assertEquals(
            15,
            jsonLogic.apply(
                """{"reduce":[{"var":"integers"}, {"+":[{"var":"current"},{"var":"accumulator"}]}, 0]}""",
                mapOf("integers" to listOf(1, 2, 3, 4, 5))
            )
        )
    }

    @Test
    fun `reduce - product`() {
        assertEquals(
            120,
            jsonLogic.apply(
                """{"reduce":[{"var":"integers"}, {"*":[{"var":"current"},{"var":"accumulator"}]}, 1]}""",
                mapOf("integers" to listOf(1, 2, 3, 4, 5))
            )
        )
    }

    @Test
    fun `reduce - empty array returns initial`() {
        assertEquals(
            0,
            jsonLogic.apply(
                """{"reduce":[{"var":"integers"}, {"+":[{"var":"current"},{"var":"accumulator"}]}, 0]}""",
                mapOf("integers" to emptyList<Int>())
            )
        )
    }

    // ==================== all tests ====================

    @Test
    fun `all - all match`() {
        assertEquals(
            true,
            jsonLogic.apply(
                """{"all":[{"var":"integers"}, {">":[{"var":""},0]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `all - not all match`() {
        assertEquals(
            false,
            jsonLogic.apply(
                """{"all":[{"var":"integers"}, {">":[{"var":""},2]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `all - empty array returns false`() {
        assertEquals(
            false,
            jsonLogic.apply(
                """{"all":[[], {">":[{"var":""},0]}]}""",
                null
            )
        )
    }

    // ==================== none tests ====================

    @Test
    fun `none - none match`() {
        assertEquals(
            true,
            jsonLogic.apply(
                """{"none":[{"var":"integers"}, {"<":[{"var":""},0]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `none - some match`() {
        assertEquals(
            false,
            jsonLogic.apply(
                """{"none":[{"var":"integers"}, {"<":[{"var":""},2]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `none - empty array returns true`() {
        assertEquals(
            true,
            jsonLogic.apply(
                """{"none":[[], {">":[{"var":""},0]}]}""",
                null
            )
        )
    }

    // ==================== some tests ====================

    @Test
    fun `some - some match`() {
        assertEquals(
            true,
            jsonLogic.apply(
                """{"some":[{"var":"integers"}, {">":[{"var":""},2]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `some - none match`() {
        assertEquals(
            false,
            jsonLogic.apply(
                """{"some":[{"var":"integers"}, {">":[{"var":""},10]}]}""",
                mapOf("integers" to listOf(1, 2, 3))
            )
        )
    }

    @Test
    fun `some - empty array returns false`() {
        assertEquals(
            false,
            jsonLogic.apply(
                """{"some":[[], {">":[{"var":""},0]}]}""",
                null
            )
        )
    }

    // ==================== merge tests ====================

    @Test
    fun `merge - two arrays`() {
        assertEquals(
            listOf(1, 2, 3, 4),
            jsonLogic.apply("""{"merge":[[1,2],[3,4]]}""", null)
        )
    }

    @Test
    fun `merge - mixed values and arrays`() {
        assertEquals(
            listOf(1, 2, 3),
            jsonLogic.apply("""{"merge":[1,[2],3]}""", null)
        )
    }

    @Test
    fun `merge - single array`() {
        assertEquals(
            listOf(1, 2, 3),
            jsonLogic.apply("""{"merge":[[1,2,3]]}""", null)
        )
    }

    // ==================== in tests ====================

    @Test
    fun `in - found in array`() {
        assertEquals(
            true,
            jsonLogic.apply("""{"in":[2, [1,2,3]]}""", null)
        )
    }

    @Test
    fun `in - not found in array`() {
        assertEquals(
            false,
            jsonLogic.apply("""{"in":[5, [1,2,3]]}""", null)
        )
    }

    @Test
    fun `in - substring found`() {
        assertEquals(
            true,
            jsonLogic.apply("""{"in":["Spring", "Springfield"]}""", null)
        )
    }

    @Test
    fun `in - substring not found`() {
        assertEquals(
            false,
            jsonLogic.apply("""{"in":["xyz", "Springfield"]}""", null)
        )
    }
}
