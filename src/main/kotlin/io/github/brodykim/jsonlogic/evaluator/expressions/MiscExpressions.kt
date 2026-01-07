package io.github.brodykim.jsonlogic.evaluator.expressions

import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import org.slf4j.LoggerFactory

/**
 * Miscellaneous expressions for JsonLogic including log.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class LogExpression : PreEvaluatedArgumentsExpression() {
    override val key = "log"

    private val logger = LoggerFactory.getLogger(LogExpression::class.java)

    override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator): Any? {
        val value = args.firstOrNull()
        logger.debug("[JsonLogic log] {}", value)
        return value
    }
}
