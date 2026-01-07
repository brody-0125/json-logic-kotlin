package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.LazyArg
import io.github.brodykim.jsonlogic.evaluator.LazyEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.asLazyArgs
import io.github.brodykim.jsonlogic.utils.TypeCoercion

/**
 * Logic expressions for JsonLogic including if, equality, comparison, and boolean operators.
 *
 * @author brody kim
 * @since 2026.01.07
 */
private fun checkAdjacentPairs(
    lazyArgs: List<LazyArg>,
    compare: (Any?, Any?) -> Boolean,
    defaultOnLessThanTwo: Boolean
): Boolean {
    if (lazyArgs.size < 2) return defaultOnLessThanTwo

    var prev = lazyArgs[0].evaluate()
    for (i in 1 until lazyArgs.size) {
        val current = lazyArgs[i].evaluate()
        if (!compare(prev, current)) {
            return false
        }
        prev = current
    }
    return true
}

class IfExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "if"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()

        if (lazyArgs.isEmpty()) {
            return null
        }

        var i = 0
        while (i < lazyArgs.size) {
            if (i == lazyArgs.size - 1) {
                return lazyArgs[i].evaluate()
            }

            val condition = lazyArgs[i].evaluate()

            if (condition is Double && condition.isNaN()) {
                throw JsonLogicThrowException(mapOf("type" to "NaN"))
            }

            if (TypeCoercion.isTruthy(condition)) {
                return if (i + 1 < lazyArgs.size) {
                    lazyArgs[i + 1].evaluate()
                } else {
                    null
                }
            }

            i += 2
        }

        return null
    }
}

class TernaryExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "?:"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return IfExpression().evaluate(args, data, evaluator)
    }
}

class EqualityExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "=="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return checkAdjacentPairs(args.asLazyArgs(), TypeCoercion::looseEquals, false)
    }
}

class StrictEqualityExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "==="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return checkAdjacentPairs(args.asLazyArgs(), TypeCoercion::strictEquals, false)
    }
}

class InequalityExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "!="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return checkAdjacentPairs(args.asLazyArgs(), { a, b -> !TypeCoercion.looseEquals(a, b) }, true)
    }
}

class StrictInequalityExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "!=="

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        return checkAdjacentPairs(args.asLazyArgs(), { a, b -> !TypeCoercion.strictEquals(a, b) }, true)
    }
}

class NotExpression : PreEvaluatedArgumentsExpression() {
    override val key = "!"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return true
        return !TypeCoercion.isTruthy(args[0])
    }
}

class DoubleNegationExpression : PreEvaluatedArgumentsExpression() {
    override val key = "!!"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.isEmpty()) return false
        return TypeCoercion.isTruthy(args[0])
    }
}

class OrExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "or"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()

        if (lazyArgs.isEmpty()) {
            return false
        }

        for (i in lazyArgs.indices) {
            val value = lazyArgs[i].evaluate()
            if (TypeCoercion.isTruthy(value)) {
                return value
            }
            if (i == lazyArgs.size - 1) {
                return value
            }
        }

        return false
    }
}

class AndExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "and"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()

        if (lazyArgs.isEmpty()) {
            return false
        }

        for (i in lazyArgs.indices) {
            val value = lazyArgs[i].evaluate()
            if (!TypeCoercion.isTruthy(value)) {
                return value
            }
            if (i == lazyArgs.size - 1) {
                return value
            }
        }

        return false
    }
}
