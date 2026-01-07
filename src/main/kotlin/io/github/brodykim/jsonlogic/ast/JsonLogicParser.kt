package io.github.brodykim.jsonlogic.ast

import io.github.brodykim.jsonlogic.JsonLogicParseException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Parser that converts JSON strings into JsonLogic AST nodes.
 *
 * @author brody kim
 * @since 2026.01.07
 */
object JsonLogicParser {
    private val logger = LoggerFactory.getLogger(JsonLogicParser::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): JsonLogicNode {
        val startTime = System.nanoTime()
        return try {
            val element = json.parseToJsonElement(jsonString)
            val node = parseElement(element)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            logger.debug("Parsed in {}ms: {} -> {}", "%.2f".format(elapsedMs), jsonString.take(50), node::class.simpleName)
            node
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            logger.warn("Parse failed in {}ms: {} - {}", "%.2f".format(elapsedMs), jsonString.take(50), e.message)
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
