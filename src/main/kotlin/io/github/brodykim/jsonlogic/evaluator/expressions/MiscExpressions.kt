package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression

/**
 * Miscellaneous expressions for JsonLogic including log.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class LogExpression : PreEvaluatedArgumentsExpression() {
    override val key = "log"

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val value = args.firstOrNull()
        println(value)
        return value
    }
}
