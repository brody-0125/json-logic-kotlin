package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.LazyEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.asLazyArgs
import io.github.brodykim.jsonlogic.utils.CollectionUtils.toListOrNull
import io.github.brodykim.jsonlogic.utils.TypeCoercion

/**
 * Array operation expressions for JsonLogic including map, filter, reduce, all, none, some, merge, and in.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class MapExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "map"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return emptyList<Any>()

        val array = toListOrNull(lazyArgs[0].evaluate()) ?: return emptyList<Any>()
        val transformNode = lazyArgs[1].astNode

        evaluator.pushScope(data)
        try {
            return array.mapIndexed { index, element ->
                evaluator.pushScope(element, index)
                try {
                    evaluator.evaluate(transformNode, element)
                } finally {
                    evaluator.popScope()
                }
            }
        } finally {
            evaluator.popScope()
        }
    }
}

class FilterExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "filter"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return emptyList<Any>()

        val array = toListOrNull(lazyArgs[0].evaluate()) ?: return emptyList<Any>()
        val conditionNode = lazyArgs[1].astNode
        val result = mutableListOf<Any?>()

        evaluator.pushScope(data)
        try {
            for ((index, element) in array.withIndex()) {
                evaluator.pushScope(element, index)
                try {
                    if (TypeCoercion.isTruthy(evaluator.evaluate(conditionNode, element))) {
                        result.add(element)
                    }
                } finally {
                    evaluator.popScope()
                }
            }
        } finally {
            evaluator.popScope()
        }

        return result
    }
}

class ReduceExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "reduce"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return null

        val arrayValue = lazyArgs[0].evaluate()
        val array = when (arrayValue) {
            is List<*> -> arrayValue
            null -> return if (lazyArgs.size >= 3) lazyArgs[2].evaluate() else null
            else -> listOf(arrayValue)
        }

        val reduceNode = lazyArgs[1].astNode

        val hasInitial = lazyArgs.size >= 3
        var accumulator: Any? = if (hasInitial) {
            lazyArgs[2].evaluate()
        } else {
            if (array.isEmpty()) return null
            array[0]
        }

        val context = mutableMapOf<String, Any?>(
            "current" to null,
            "accumulator" to null
        )

        evaluator.pushScope(data)
        try {
            val startIndex = if (hasInitial) 0 else 1
            for (i in startIndex until array.size) {
                context["current"] = array[i]
                context["accumulator"] = accumulator
                evaluator.pushScope(context, i)
                try {
                    accumulator = evaluator.evaluate(reduceNode, context)
                } finally {
                    evaluator.popScope()
                }
            }
        } finally {
            evaluator.popScope()
        }

        return accumulator
    }
}

class AllExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "all"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return false

        val array = toListOrNull(lazyArgs[0].evaluate()) ?: return false

        if (array.isEmpty()) return false

        val conditionNode = lazyArgs[1].astNode

        evaluator.pushScope(data)
        try {
            for ((index, element) in array.withIndex()) {
                evaluator.pushScope(element, index)
                try {
                    if (!TypeCoercion.isTruthy(evaluator.evaluate(conditionNode, element))) {
                        return false
                    }
                } finally {
                    evaluator.popScope()
                }
            }
            return true
        } finally {
            evaluator.popScope()
        }
    }
}

class NoneExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "none"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return true

        val array = toListOrNull(lazyArgs[0].evaluate()) ?: return true

        if (array.isEmpty()) return true

        val conditionNode = lazyArgs[1].astNode

        evaluator.pushScope(data)
        try {
            for ((index, element) in array.withIndex()) {
                evaluator.pushScope(element, index)
                try {
                    if (TypeCoercion.isTruthy(evaluator.evaluate(conditionNode, element))) {
                        return false
                    }
                } finally {
                    evaluator.popScope()
                }
            }
            return true
        } finally {
            evaluator.popScope()
        }
    }
}

class SomeExpression : LazyEvaluatedArgumentsExpression() {
    override val key = "some"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val lazyArgs = args.asLazyArgs()
        if (lazyArgs.size < 2) return false

        val array = toListOrNull(lazyArgs[0].evaluate()) ?: return false

        if (array.isEmpty()) return false

        val conditionNode = lazyArgs[1].astNode

        evaluator.pushScope(data)
        try {
            for ((index, element) in array.withIndex()) {
                evaluator.pushScope(element, index)
                try {
                    if (TypeCoercion.isTruthy(evaluator.evaluate(conditionNode, element))) {
                        return true
                    }
                } finally {
                    evaluator.popScope()
                }
            }
            return false
        } finally {
            evaluator.popScope()
        }
    }
}

class MergeExpression : PreEvaluatedArgumentsExpression() {
    override val key = "merge"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val result = mutableListOf<Any?>()

        for (arg in args) {
            when (arg) {
                is List<*> -> result.addAll(arg)
                else -> result.add(arg)
            }
        }

        return result
    }
}

class InExpression : PreEvaluatedArgumentsExpression() {
    override val key = "in"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        if (args.size < 2) return false

        val needle = args[0]
        val haystack = args[1]

        return when (haystack) {
            is String -> {
                val needleStr = needle?.toString() ?: return false
                haystack.contains(needleStr)
            }
            is List<*> -> {
                haystack.any { TypeCoercion.looseEquals(it, needle) }
            }
            else -> false
        }
    }
}
