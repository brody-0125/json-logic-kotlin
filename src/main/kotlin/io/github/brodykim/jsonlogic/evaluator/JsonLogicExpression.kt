package io.github.brodykim.jsonlogic.evaluator

/**
 * Interface and base classes for JsonLogic expression implementations.
 *
 * @author brody kim
 * @since 2026.01.07
 */
interface JsonLogicExpression {
    val key: String
    fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any?
}

abstract class PreEvaluatedArgumentsExpression : JsonLogicExpression

abstract class LazyEvaluatedArgumentsExpression : JsonLogicExpression

@Suppress("UNCHECKED_CAST")
internal fun List<Any?>.asLazyArgs(): List<LazyArg> = this as List<LazyArg>
