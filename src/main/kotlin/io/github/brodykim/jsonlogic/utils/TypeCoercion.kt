package io.github.brodykim.jsonlogic.utils

import io.github.brodykim.jsonlogic.JsonLogicTypeCoercionException

/**
 * Type coercion utilities for JsonLogic evaluation implementing JavaScript-like type coercion rules.
 *
 * @author brody kim
 * @since 2026.01.07
 */
sealed class CoercionResult<out T> {
    data class Success<T>(val value: T) : CoercionResult<T>()
    data class Failed(val originalValue: Any?, val reason: String) : CoercionResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failed -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failed -> throw JsonLogicTypeCoercionException(
            "Type coercion failed: $reason",
            originalValue,
            "Number"
        )
    }
}

object TypeCoercion {

    fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> {
                val d = value.toDouble()
                !d.isNaN() && d != 0.0
            }
            is String -> value.isNotEmpty()
            is Map<*, *> -> true
            is Collection<*> -> value.isNotEmpty()
            is Array<*> -> value.isNotEmpty()
            else -> true
        }
    }

    fun toNumber(value: Any?): Double {
        return when (value) {
            null -> 0.0
            is Number -> value.toDouble()
            is Boolean -> if (value) 1.0 else 0.0
            is String -> {
                val trimmed = value.trim()
                if (trimmed.isEmpty()) 0.0
                else trimmed.toDoubleOrNull() ?: Double.NaN
            }
            is Collection<*> -> {
                if (value.isEmpty()) 0.0
                else if (value.size == 1) toNumber(value.first())
                else Double.NaN
            }
            else -> Double.NaN
        }
    }

    fun toNumberResult(value: Any?): CoercionResult<Double> {
        return when (value) {
            null -> CoercionResult.Success(0.0)
            is Number -> CoercionResult.Success(value.toDouble())
            is Boolean -> CoercionResult.Success(if (value) 1.0 else 0.0)
            is String -> {
                val trimmed = value.trim()
                if (trimmed.isEmpty()) {
                    CoercionResult.Success(0.0)
                } else {
                    val parsed = trimmed.toDoubleOrNull()
                    if (parsed != null) {
                        CoercionResult.Success(parsed)
                    } else {
                        CoercionResult.Failed(value, "Cannot parse string '$trimmed' as number")
                    }
                }
            }
            is Collection<*> -> {
                when {
                    value.isEmpty() -> CoercionResult.Success(0.0)
                    value.size == 1 -> toNumberResult(value.first())
                    else -> CoercionResult.Failed(value, "Cannot convert multi-element collection to number")
                }
            }
            else -> CoercionResult.Failed(value, "Cannot convert ${value::class.simpleName} to number")
        }
    }

    fun toNumberStrict(value: Any?): Double {
        return toNumberResult(value).getOrThrow()
    }

    fun formatNumber(value: Number): String {
        val d = value.toDouble()
        if (!d.isFinite()) {
            return d.toString()
        }
        val longValue = d.toLong()
        return if (d == longValue.toDouble()) {
            longValue.toString()
        } else {
            d.toString()
        }
    }

    fun toString(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> value
            is Boolean -> value.toString()
            is Number -> formatNumber(value)
            is Collection<*> -> value.joinToString(",") { toString(it) }
            is Array<*> -> value.joinToString(",") { toString(it) }
            else -> value.toString()
        }
    }

    fun looseEquals(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null && b == null) return true

        if (a == null && b is Number && b.toDouble() == 0.0) return true
        if (b == null && a is Number && a.toDouble() == 0.0) return true

        if (a == null || b == null) return false

        if (a::class == b::class) {
            return when (a) {
                is Number -> a.toDouble() == (b as Number).toDouble()
                else -> a == b
            }
        }

        if (a is Number && b is Number) {
            return a.toDouble() == b.toDouble()
        }

        if (a is Boolean || b is Boolean) {
            return toNumber(a) == toNumber(b)
        }

        if ((a is String && b is Number) || (a is Number && b is String)) {
            val numA = toNumber(a)
            val numB = toNumber(b)
            if (numA.isNaN() || numB.isNaN()) return false
            return numA == numB
        }

        return a == b
    }

    fun strictEquals(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null && b == null) return true
        if (a == null || b == null) return false

        if (a::class != b::class) {
            if (a is Number && b is Number) {
                return a.toDouble() == b.toDouble()
            }
            return false
        }

        return when (a) {
            is Number -> a.toDouble() == (b as Number).toDouble()
            else -> a == b
        }
    }

    fun compareNumeric(a: Any?, b: Any?): Int? {
        if (a is String && b is String) {
            return a.compareTo(b)
        }

        val numA = toNumber(a)
        val numB = toNumber(b)

        if (!numA.isNaN() && !numB.isNaN()) {
            return when {
                numA < numB -> -1
                numA > numB -> 1
                else -> 0
            }
        }

        val strA = a?.toString() ?: return null
        val strB = b?.toString() ?: return null
        return strA.compareTo(strB)
    }

    fun unwrap(value: Any?): Any? {
        return when (value) {
            is List<*> -> if (value.size == 1) unwrap(value[0]) else value
            is Array<*> -> if (value.size == 1) unwrap(value[0]) else value
            else -> value
        }
    }
}
