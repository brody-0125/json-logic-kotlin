package io.github.brodykim.jsonlogic.ast

/**
 * Sealed interface hierarchy for all JsonLogic AST nodes.
 *
 * @author brody kim
 * @since 2026.01.07
 */
sealed interface JsonLogicNode

data object JsonLogicNull : JsonLogicNode

data class JsonLogicBoolean(val value: Boolean) : JsonLogicNode

data class JsonLogicNumber(val value: Double) : JsonLogicNode {
    constructor(value: Int) : this(value.toDouble())
    constructor(value: Long) : this(value.toDouble())
}

data class JsonLogicString(val value: String) : JsonLogicNode

data class JsonLogicArray(val value: List<JsonLogicNode>) : JsonLogicNode {
    constructor(vararg nodes: JsonLogicNode) : this(nodes.toList())

    val size: Int get() = value.size
    operator fun get(index: Int): JsonLogicNode = value[index]
    fun isEmpty(): Boolean = value.isEmpty()
}

data class JsonLogicMap(val value: Map<String, JsonLogicNode>) : JsonLogicNode {
    fun isEmpty(): Boolean = value.isEmpty()
}

data class JsonLogicOperation(
    val operator: String,
    val arguments: JsonLogicArray
) : JsonLogicNode {
    constructor(operator: String, vararg args: JsonLogicNode) : this(operator, JsonLogicArray(args.toList()))
}
