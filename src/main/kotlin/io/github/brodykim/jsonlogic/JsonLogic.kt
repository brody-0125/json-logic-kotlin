package io.github.brodykim.jsonlogic

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Cache
import io.github.brodykim.jsonlogic.ast.JsonLogicNode
import io.github.brodykim.jsonlogic.ast.JsonLogicParser
import io.github.brodykim.jsonlogic.evaluator.JsonLogicEvaluator
import io.github.brodykim.jsonlogic.evaluator.JsonLogicExpression
import io.github.brodykim.jsonlogic.evaluator.PreEvaluatedArgumentsExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.AddExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.AllExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.AndExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.CatExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.CoalesceExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.DivideExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.DoubleNegationExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.EqualityExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.ExistsExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.FilterExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.GreaterThanExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.GreaterThanOrEqualExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.IfExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.InExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.InequalityExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.LessThanExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.LessThanOrEqualExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.LogExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MapExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MaxExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MergeExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MinExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MissingExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MissingSomeExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.ModuloExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.MultiplyExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.NoneExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.NotExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.OrExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.ReduceExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.SomeExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.StrictEqualityExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.StrictInequalityExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.SubstrExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.SubtractExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.TernaryExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.ThrowExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.TryExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.ValExpression
import io.github.brodykim.jsonlogic.evaluator.expressions.VarExpression
import io.github.brodykim.jsonlogic.utils.TypeCoercion
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Main entry point for JsonLogic evaluation providing thread-safe and reusable API.
 *
 * @author brody kim
 * @since 2026.01.07
 */
class JsonLogic(private val maxCacheSize: Int = DEFAULT_CACHE_SIZE) {

    private val parseCache: Cache<String, JsonLogicNode> = Caffeine.newBuilder()
        .maximumSize(maxCacheSize.toLong())
        .build()

    private val expressions = ConcurrentHashMap<String, JsonLogicExpression>()

    private val evaluatorLock = ReentrantReadWriteLock()

    @Volatile
    private var evaluator: JsonLogicEvaluator? = null

    @Volatile
    private var expressionVersion: Long = 0

    @Volatile
    private var evaluatorVersion: Long = -1

    init {
        registerBuiltInOperationsInternal()
        evaluatorVersion = expressionVersion
    }

    private fun registerBuiltInOperationsInternal() {
        addOperationInternal(VarExpression())
        addOperationInternal(MissingExpression())
        addOperationInternal(MissingSomeExpression())

        addOperationInternal(IfExpression())
        addOperationInternal(TernaryExpression())
        addOperationInternal(EqualityExpression())
        addOperationInternal(StrictEqualityExpression())
        addOperationInternal(InequalityExpression())
        addOperationInternal(StrictInequalityExpression())
        addOperationInternal(NotExpression())
        addOperationInternal(DoubleNegationExpression())
        addOperationInternal(OrExpression())
        addOperationInternal(AndExpression())

        addOperationInternal(GreaterThanExpression())
        addOperationInternal(GreaterThanOrEqualExpression())
        addOperationInternal(LessThanExpression())
        addOperationInternal(LessThanOrEqualExpression())
        addOperationInternal(MaxExpression())
        addOperationInternal(MinExpression())
        addOperationInternal(AddExpression())
        addOperationInternal(SubtractExpression())
        addOperationInternal(MultiplyExpression())
        addOperationInternal(DivideExpression())
        addOperationInternal(ModuloExpression())

        addOperationInternal(MapExpression())
        addOperationInternal(FilterExpression())
        addOperationInternal(ReduceExpression())
        addOperationInternal(AllExpression())
        addOperationInternal(NoneExpression())
        addOperationInternal(SomeExpression())
        addOperationInternal(MergeExpression())
        addOperationInternal(InExpression())

        addOperationInternal(CatExpression())
        addOperationInternal(SubstrExpression())

        addOperationInternal(LogExpression())

        addOperationInternal(ValExpression())
        addOperationInternal(CoalesceExpression())
        addOperationInternal(ExistsExpression())
        addOperationInternal(ThrowExpression())
        addOperationInternal(TryExpression())
    }

    private fun addOperationInternal(expression: JsonLogicExpression) {
        expressions[expression.key] = expression
    }

    fun addOperation(expression: JsonLogicExpression) {
        evaluatorLock.write {
            expressions[expression.key] = expression
            expressionVersion++
        }
    }

    fun addOperation(key: String, func: (args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator) -> Any?) {
        addOperation(object : PreEvaluatedArgumentsExpression() {
            override val key = key
            override fun evaluate(args: List<Any?>, data: Any?, evaluator: JsonLogicEvaluator) =
                func(args, data, evaluator)
        })
    }

    fun apply(rules: String, data: Any?): Any? {
        val node = parseCache.get(rules) { JsonLogicParser.parse(it) }
        return apply(node, data)
    }

    fun apply(rules: JsonLogicNode, data: Any?): Any? {
        val eval = getOrCreateEvaluator()
        return eval.evaluate(rules, data)
    }

    private fun getOrCreateEvaluator(): JsonLogicEvaluator {
        val currentVersion = expressionVersion
        val currentEvaluator = evaluator

        if (currentEvaluator != null && evaluatorVersion == currentVersion) {
            return currentEvaluator
        }

        return evaluatorLock.write {
            val eval = evaluator
            if (eval != null && evaluatorVersion == expressionVersion) {
                return@write eval
            }

            val newEvaluator = JsonLogicEvaluator(expressions.toMap())
            evaluator = newEvaluator
            evaluatorVersion = expressionVersion
            newEvaluator
        }
    }

    fun applyJson(rules: String, dataJson: String?): Any? {
        val data = if (dataJson != null) {
            parseJsonToKotlin(dataJson)
        } else {
            null
        }
        return apply(rules, data)
    }

    private fun parseJsonToKotlin(json: String): Any? {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
        return jsonElementToKotlin(element)
    }

    private fun jsonElementToKotlin(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    else -> {
                        val content = element.content
                        content.toIntOrNull()
                            ?: content.toLongOrNull()
                            ?: content.toDoubleOrNull()
                            ?: content
                    }
                }
            }
            is JsonArray -> element.map { jsonElementToKotlin(it) }
            is JsonObject -> element.mapValues { jsonElementToKotlin(it.value) }
        }
    }

    fun clearCache() {
        parseCache.invalidateAll()
    }

    fun cacheSize(): Long = parseCache.estimatedSize()

    companion object {
        const val DEFAULT_CACHE_SIZE = 1000

        @JvmStatic
        fun truthy(value: Any?): Boolean {
            return TypeCoercion.isTruthy(value)
        }
    }
}
