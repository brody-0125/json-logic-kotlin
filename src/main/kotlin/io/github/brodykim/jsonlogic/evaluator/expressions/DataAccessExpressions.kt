package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.utils.CollectionUtils

/**
 * Data access expressions for JsonLogic including var, missing, and missing_some.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class VarExpression : PreEvaluatedArgumentsExpression() {
    override val key = "var"

    companion object {
        internal val INSTANCE = VarExpression()
    }

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) {
            return data
        }

        val path = args[0]
        val defaultValue = if (args.size > 1) args[1] else null

        if (path == "" || path == null) {
            return data
        }

        if (path is Number) {
            return CollectionUtils.getByIndex(data, path.toInt()) ?: defaultValue
        }

        if (path is String) {
            return getByPath(data, path) ?: defaultValue
        }

        return defaultValue
    }

    private fun getByPath(data: Any?, path: String): Any? {
        if (data == null) return null
        if (path.isEmpty()) return data

        val parts = path.split(".")
        var current: Any? = data

        for (part in parts) {
            current = when (current) {
                null -> return null
                is Map<*, *> -> current[part]
                is List<*> -> {
                    val index = part.toIntOrNull()
                    if (index != null && index >= 0 && index < current.size) {
                        current[index]
                    } else {
                        null
                    }
                }
                else -> {
                    val index = part.toIntOrNull()
                    if (index != null) {
                        CollectionUtils.getByIndex(current, index)
                    } else {
                        null
                    }
                }
            }
        }

        return current
    }
}

class MissingExpression : PreEvaluatedArgumentsExpression() {
    override val key = "missing"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val missing = mutableListOf<String>()

        val keys = CollectionUtils.flattenArgs(args)

        for (keyArg in keys) {
            val key = keyArg?.toString() ?: continue
            val value = VarExpression.INSTANCE.evaluate(listOf(key), data, evaluator)
            if (value == null || value == "") {
                missing.add(key)
            }
        }

        return missing
    }
}

class MissingSomeExpression : PreEvaluatedArgumentsExpression() {
    override val key = "missing_some"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.size < 2) {
            return emptyList<String>()
        }

        val minimum = when (val first = args[0]) {
            is Number -> first.toInt()
            else -> return emptyList<String>()
        }

        val keys = when (val second = args[1]) {
            is List<*> -> second
            else -> listOf(second)
        }

        val missing = mutableListOf<String>()
        var present = 0

        for (keyArg in keys) {
            val key = keyArg?.toString() ?: continue
            val value = VarExpression.INSTANCE.evaluate(listOf(key), data, evaluator)
            if (value == null || value == "") {
                missing.add(key)
            } else {
                present++
            }
        }

        return if (present >= minimum) {
            emptyList<String>()
        } else {
            missing
        }
    }
}
