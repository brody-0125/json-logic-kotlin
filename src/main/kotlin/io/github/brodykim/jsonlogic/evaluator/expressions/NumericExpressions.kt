package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.LazyEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.asLazyArgs
import io.github.brodykim.jsonlogic.utils.CollectionUtils
import io.github.brodykim.jsonlogic.utils.TypeCoercion

/**
 * Numeric expressions for JsonLogic including comparison, arithmetic, and math operations.
 *
 * @author brody kim
 * @since 2026.01.07
 */
private fun formatNumber(value: Double): Number {
    return if (value.isFinite() && value == value.toLong().toDouble()) {
        if (value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) {
            value.toInt()
        } else {
            value.toLong()
        }
    } else {
        value
    }
}

private enum class CompareMode {
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL
}

private fun evaluateChainedComparisonLazy(
    args: List<Any?>,
    mode: CompareMode
): Boolean {
    val lazyArgs = args.asLazyArgs()
    if (lazyArgs.size < 2) return false

    val evaluated = mutableListOf<Any?>()

    for (i in 0 until lazyArgs.size - 1) {
        while (evaluated.size <= i + 1) {
            evaluated.add(lazyArgs[evaluated.size].evaluate())
        }

        val cmp = TypeCoercion.compareNumeric(evaluated[i], evaluated[i + 1]) ?: return false
        val passes = when (mode) {
            CompareMode.GREATER_THAN -> cmp > 0
            CompareMode.GREATER_OR_EQUAL -> cmp >= 0
            CompareMode.LESS_THAN -> cmp < 0
            CompareMode.LESS_OR_EQUAL -> cmp <= 0
        }
        if (!passes) return false
    }
    return true
}

class GreaterThanExpression : LazyEvaluatedArgumentsExpression() {
    override val key = ">"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return evaluateChainedComparisonLazy(args, CompareMode.GREATER_THAN)
    }
}

class GreaterThanOrEqualExpression : LazyEvaluatedArgumentsExpression() {
    override val key = ">="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return evaluateChainedComparisonLazy(args, CompareMode.GREATER_OR_EQUAL)
    }
}

class LessThanExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "<"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return evaluateChainedComparisonLazy(args, CompareMode.LESS_THAN)
    }
}

class LessThanOrEqualExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "<="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return evaluateChainedComparisonLazy(args, CompareMode.LESS_OR_EQUAL)
    }
}

private fun findExtremum(args: List<Any?>, selector: (Double, Double) -> Double): Any? {
    val flatArgs = CollectionUtils.flattenArgs(args)
    if (flatArgs.isEmpty()) return null

    var result: Double? = null
    for (arg in flatArgs) {
        val num = TypeCoercion.toNumber(arg)
        if (!num.isNaN()) {
            result = if (result == null) num else selector(result, num)
        }
    }

    return result?.let { formatNumber(it) }
}

class MaxExpression : PreEvaluatedArgumentsExpression() {
    override val key = "max"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return findExtremum(args, ::maxOf)
    }
}

class MinExpression : PreEvaluatedArgumentsExpression() {
    override val key = "min"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return findExtremum(args, ::minOf)
    }
}

class AddExpression : PreEvaluatedArgumentsExpression() {
    override val key = "+"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val flatArgs = CollectionUtils.flattenArgs(args)

        if (flatArgs.isEmpty()) return 0

        if (flatArgs.size == 1) {
            return formatNumber(TypeCoercion.toNumber(flatArgs[0]))
        }

        var sum = 0.0
        for (arg in flatArgs) {
            sum += TypeCoercion.toNumber(arg)
        }

        return formatNumber(sum)
    }
}

class SubtractExpression : PreEvaluatedArgumentsExpression() {
    override val key = "-"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return null

        if (args.size == 1) {
            return formatNumber(-TypeCoercion.toNumber(args[0]))
        }

        var result = TypeCoercion.toNumber(args[0])
        for (i in 1 until args.size) {
            result -= TypeCoercion.toNumber(args[i])
        }
        return formatNumber(result)
    }
}

class MultiplyExpression : PreEvaluatedArgumentsExpression() {
    override val key = "*"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val flatArgs = CollectionUtils.flattenArgs(args)

        if (flatArgs.isEmpty()) return 1

        var product = 1.0
        for (arg in flatArgs) {
            product *= TypeCoercion.toNumber(arg)
        }

        return formatNumber(product)
    }
}

class DivideExpression : PreEvaluatedArgumentsExpression() {
    override val key = "/"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return null

        if (args.size == 1) {
            val n = TypeCoercion.toNumber(args[0])
            if (n == 0.0) return Double.NaN
            return formatNumber(1.0 / n)
        }

        var result = TypeCoercion.toNumber(args[0])
        for (i in 1 until args.size) {
            val divisor = TypeCoercion.toNumber(args[i])
            if (divisor == 0.0) {
                return Double.NaN
            } else {
                result /= divisor
            }
        }

        return formatNumber(result)
    }
}

class ModuloExpression : PreEvaluatedArgumentsExpression() {
    override val key = "%"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.size < 2) return null

        var result = TypeCoercion.toNumber(args[0])
        for (i in 1 until args.size) {
            result %= TypeCoercion.toNumber(args[i])
        }

        return formatNumber(result)
    }
}
