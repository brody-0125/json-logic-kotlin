package io.github.brodykim.jsonlogic

/**
 * Exception hierarchy for JsonLogic errors.
 *
 * @author brody kim
 * @since 2026.01.07
 */
open class JsonLogicException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class JsonLogicParseException(
    message: String,
    cause: Throwable? = null
) : JsonLogicException(message, cause)

class JsonLogicUnknownOperatorException(
    val operator: String,
    cause: Throwable? = null
) : JsonLogicException("Unknown operator: $operator", cause)

class JsonLogicTypeCoercionException(
    message: String,
    val originalValue: Any? = null,
    val targetType: String? = null,
    cause: Throwable? = null
) : JsonLogicException(message, cause)

class JsonLogicEvaluationException(
    message: String,
    cause: Throwable? = null
) : JsonLogicException(message, cause)
