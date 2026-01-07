package io.github.brodykim.jsonlogic.utils

/**
 * Utility functions for collection operations used across expressions.
 *
 * @author brody kim
 * @since 2026.01.07
 */
object CollectionUtils {

    fun toListOrNull(value: Any?): List<*>? {
        return when (value) {
            is List<*> -> value
            null -> null
            else -> listOf(value)
        }
    }

    fun getByIndex(data: Any?, index: Int): Any? {
        return when (data) {
            is List<*> -> if (index >= 0 && index < data.size) data[index] else null
            is Array<*> -> if (index >= 0 && index < data.size) data[index] else null
            is String -> if (index >= 0 && index < data.length) data[index].toString() else null
            else -> null
        }
    }

    fun flattenArgs(args: List<Any?>, includeNulls: Boolean = false): List<Any?> {
        val result = mutableListOf<Any?>()
        val stack = ArrayDeque<Any?>()

        for (arg in args.asReversed()) {
            stack.addLast(arg)
        }

        while (stack.isNotEmpty()) {
            when (val item = stack.removeLast()) {
                is List<*> -> {
                    for (element in item.asReversed()) {
                        if (includeNulls || element != null) {
                            stack.addLast(element)
                        }
                    }
                }
                else -> result.add(item)
            }
        }
        return result
    }

}
