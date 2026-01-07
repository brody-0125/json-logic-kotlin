package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.JsonLogicException
import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.LazyEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.asLazyArgs
import io.github.brodykim.jsonlogic.utils.CollectionUtils

/**
 * Extended expressions for JsonLogic including val, coalesce, exists, throw, and try.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class JsonLogicThrowException(val errorValue: Any?) : JsonLogicException(
    when (errorValue) {
        is Map<*, *> -> errorValue["type"]?.toString() ?: errorValue.toString()
        else -> errorValue?.toString() ?: "null"
    }
) {
    fun toErrorObject(): Map<String, Any?> {
        return when (errorValue) {
            is Map<*, *> -> @Suppress("UNCHECKED_CAST") (errorValue as Map<String, Any?>)
            else -> mapOf("type" to errorValue)
        }
    }
}

class ValExpression : PreEvaluatedArgumentsExpression() {
    override val key = "val"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) {
            return data
        }

        val firstArg = args[0]
        if (firstArg is List<*> && firstArg.size == 1) {
            val scopeLevel = when (val n = firstArg[0]) {
                is Number -> n.toInt()
                else -> null
            }
            if (scopeLevel != null) {
                val path = args.drop(1)
                return evaluateWithScope(evaluator, scopeLevel, path, data)
            }
        }

        if (args.size == 1) {
            when (firstArg) {
                null -> return data
                is String -> return getByKey(data, firstArg)
                is Number -> return CollectionUtils.getByIndex(data, firstArg.toInt())
                is List<*> -> {
                    if (firstArg.isEmpty()) return data
                    return traversePath(data, firstArg)
                }
                else -> return data
            }
        }

        return traversePath(data, args)
    }

    private fun evaluateWithScope(
        evaluator: JsonLogicEvaluator,
        scopeLevel: Int,
        path: List<*>,
        @Suppress("UNUSED_PARAMETER") currentData: Any?
    ): Any? {
        if (path.size == 1 && path[0] == "index") {
            return evaluator.getScopeIndex(scopeLevel)
        }

        val scopeData = evaluator.getScopeData(scopeLevel) ?: return null

        if (path.isEmpty()) {
            return scopeData
        }

        return traversePath(scopeData, path)
    }

    private fun traversePath(data: Any?, path: List<*>): Any? {
        var current: Any? = data
        for (segment in path) {
            if (current == null) return null
            current = when (segment) {
                is Number -> CollectionUtils.getByIndex(current, segment.toInt())
                is String -> getByKey(current, segment)
                else -> null
            }
        }
        return current
    }

    private fun getByKey(data: Any?, key: String): Any? {
        return when (data) {
            null -> null
            is Map<*, *> -> data[key]
            is List<*> -> {
                val index = key.toIntOrNull()
                if (index != null && index >= 0 && index < data.size) data[index] else null
            }
            else -> null
        }
    }
}

class CoalesceExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "??"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()

        for (lazyArg in lazyArgs) {
            val value = lazyArg.evaluate()
            if (value != null) {
                return value
            }
        }

        return null
    }
}

class ExistsExpression : PreEvaluatedArgumentsExpression() {
    override val key = "exists"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return false

        val path: List<Any?> = if (args.size > 1) {
            args
        } else {
            when (val first = args[0]) {
                is String -> listOf(first)
                is List<*> -> first
                else -> return false
            }
        }

        return checkExists(data, path)
    }

    private fun checkExists(data: Any?, path: List<Any?>): Boolean {
        if (path.isEmpty()) return true
        if (data == null) return false

        var current: Any? = data
        for (segment in path) {
            val key = segment?.toString() ?: return false
            when (current) {
                is Map<*, *> -> {
                    if (!current.containsKey(key)) return false
                    current = current[key]
                }
                is List<*> -> {
                    val index = key.toIntOrNull() ?: return false
                    if (index < 0 || index >= current.size) return false
                    current = current[index]
                }
                else -> return false
            }
        }
        return true
    }
}

class ThrowExpression : PreEvaluatedArgumentsExpression() {
    override val key = "throw"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val errorValue = if (args.isNotEmpty()) args[0] else null
        throw JsonLogicThrowException(errorValue)
    }
}

class TryExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "try"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.isEmpty()) return null

        var lastError: JsonLogicThrowException? = null

        for (lazyArg in lazyArgs) {
            try {
                val result = if (lastError != null) {
                    val errorContext = lastError.toErrorObject()
                    evaluator.pushScope(data)
                    evaluator.pushScope(errorContext)
                    try {
                        lazyArg.evaluateWith(errorContext)
                    } finally {
                        evaluator.popScope()
                        evaluator.popScope()
                    }
                } else {
                    lazyArg.evaluate()
                }

                if (containsNaN(result)) {
                    lastError = JsonLogicThrowException(mapOf("type" to "NaN"))
                    continue
                }

                return result
            } catch (e: JsonLogicThrowException) {
                lastError = e
                continue
            }
        }

        if (lastError != null) {
            throw lastError
        }

        return null
    }

    companion object {
        private const val MAX_NAN_CHECK_DEPTH = 100
    }

    private fun containsNaN(value: Any?): Boolean {
        return containsNaN(value, 0)
    }

    private fun containsNaN(value: Any?, depth: Int): Boolean {
        if (depth > MAX_NAN_CHECK_DEPTH) return false

        return when (value) {
            is Double -> value.isNaN()
            is Float -> value.isNaN()
            is List<*> -> value.any { containsNaN(it, depth + 1) }
            is Array<*> -> value.any { containsNaN(it, depth + 1) }
            is Map<*, *> -> value.values.any { containsNaN(it, depth + 1) }
            else -> false
        }
    }
}
