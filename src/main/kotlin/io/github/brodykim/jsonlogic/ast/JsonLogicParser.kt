package io.github.brodykim.jsonlogic.ast

import io.github.brodykim.jsonlogic.JsonLogicParseException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parser that converts JSON strings into JsonLogic AST nodes.
 *
 * @author brody kim
 * @since 2026.01.07
 */
object JsonLogicParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): JsonLogicNode {
        return try {
            val element = json.parseToJsonElement(jsonString)
            parseElement(element)
        } catch (e: Exception) {
            when (e) {
                is JsonLogicParseException -> throw e
                else -> throw JsonLogicParseException("Failed to parse JSON: ${e.message}", e)
            }
        }
    }

    fun parseElement(element: JsonElement): JsonLogicNode {
        return when (element) {
            is JsonNull -> JsonLogicNull
            is JsonPrimitive -> parsePrimitive(element)
            is JsonArray -> parseArray(element)
            is JsonObject -> parseObject(element)
        }
    }

    private fun parsePrimitive(primitive: JsonPrimitive): JsonLogicNode {
        return when {
            primitive.isString -> JsonLogicString(primitive.content)
            primitive.content == "true" -> JsonLogicBoolean(true)
            primitive.content == "false" -> JsonLogicBoolean(false)
            primitive.content == "null" -> JsonLogicNull
            else -> {
                val content = primitive.content
                val number = content.toDoubleOrNull()
                    ?: throw JsonLogicParseException("Invalid primitive value: $content")
                JsonLogicNumber(number)
            }
        }
    }

    private fun parseArray(array: JsonArray): JsonLogicArray {
        return JsonLogicArray(array.map { parseElement(it) })
    }

    private fun parseObject(obj: JsonObject): JsonLogicNode {
        if (obj.isEmpty()) {
            return JsonLogicMap(emptyMap())
        }

        if (obj.size != 1) {
            return JsonLogicMap(
                obj.entries.associate { (key, value) ->
                    key to parseElement(value)
                }
            )
        }

        val (operator, value) = obj.entries.first()

        val arguments = when (value) {
            is JsonArray -> parseArray(value)
            else -> JsonLogicArray(listOf(parseElement(value)))
        }

        return JsonLogicOperation(operator, arguments)
    }
}
