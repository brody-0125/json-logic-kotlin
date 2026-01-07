package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.utils.TypeCoercion

/**
 * String expressions for JsonLogic including cat and substr.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class CatExpression : PreEvaluatedArgumentsExpression() {
    override val key = "cat"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return ""

        return buildString {
            appendValue(args)
        }
    }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> { }
            is List<*> -> {
                for (item in value) {
                    appendValue(item)
                }
            }
            is String -> append(value)
            is Number -> append(TypeCoercion.formatNumber(value))
            is Boolean -> append(value)
            else -> append(value)
        }
    }
}

class SubstrExpression : PreEvaluatedArgumentsExpression() {
    override val key = "substr"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return ""

        val str = when (val first = args[0]) {
            null -> return ""
            is String -> first
            is Number -> TypeCoercion.formatNumber(first)
            else -> first.toString()
        }

        if (args.size < 2) return str

        val startArg = TypeCoercion.toNumber(args[1]).toInt()

        val start = if (startArg < 0) {
            maxOf(0, str.length + startArg)
        } else {
            minOf(startArg, str.length)
        }

        if (args.size < 3) {
            return str.substring(start)
        }

        val lengthArg = TypeCoercion.toNumber(args[2]).toInt()

        val end = if (lengthArg < 0) {
            maxOf(start, str.length + lengthArg)
        } else {
            minOf(start + lengthArg, str.length)
        }

        return if (start >= end) "" else str.substring(start, end)
    }
}
