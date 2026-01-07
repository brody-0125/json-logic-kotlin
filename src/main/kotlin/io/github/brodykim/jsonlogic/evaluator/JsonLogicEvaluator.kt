package io.github.brodykim.jsonlogic.evaluator

import io.github.brodykim.jsonlogic.JsonLogicUnknownOperatorException
import io.github.brodykim.jsonlogic.ast.JsonLogicArray
import io.github.brodykim.jsonlogic.ast.JsonLogicBoolean
import io.github.brodykim.jsonlogic.ast.JsonLogicMap
import io.github.brodykim.jsonlogic.ast.JsonLogicNode
import io.github.brodykim.jsonlogic.ast.JsonLogicNull
import io.github.brodykim.jsonlogic.ast.JsonLogicNumber
import io.github.brodykim.jsonlogic.ast.JsonLogicOperation
import io.github.brodykim.jsonlogic.ast.JsonLogicString
import io.github.brodykim.jsonlogic.utils.TypeCoercion

/**
 * Core evaluation engine for JsonLogic that evaluates AST nodes against provided data.
 *
 * @author brody kim
 * @since 2026.01.07
 */
data class ScopeFrame(
    val data: Any?,
    val index: Int = -1
)

class JsonLogicEvaluator(
    private val expressions: Map<String, JsonLogicExpression>
) {
    private val scopeStack = ThreadLocal.withInitial { mutableListOf<ScopeFrame>() }

    fun getScopeStack(): List<ScopeFrame> = scopeStack.get()

    fun pushScope(data: Any?, index: Int = -1) {
        scopeStack.get().add(ScopeFrame(data, index))
    }

    fun popScope() {
        val stack = scopeStack.get()
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
        }
    }

    fun getScopeData(level: Int): Any? {
        val stack = scopeStack.get()
        if (stack.isEmpty()) return null

        return if (level >= 0) {
            if (level == 0) {
                stack.firstOrNull()?.data
            } else {
                val index = stack.size - level
                if (index >= 0 && index < stack.size) stack[index].data else null
            }
        } else {
            val index = stack.size + level
            if (index >= 0 && index < stack.size) stack[index].data else null
        }
    }

    fun getScopeIndex(level: Int): Int {
        val stack = scopeStack.get()
        if (stack.isEmpty()) return -1

        val index = if (level >= 0) {
            if (level == 0) 0 else stack.size - level
        } else {
            stack.size + level
        }
        return if (index >= 0 && index < stack.size) stack[index].index else -1
    }

    companion object {
        private val threadLocalPool = ThreadLocal<LazyArgPool>()

        @JvmStatic
        fun cleanupThreadLocal() {
            threadLocalPool.get()?.clear()
            threadLocalPool.remove()
        }
    }

    private fun getPool(): LazyArgPool {
        var pool = threadLocalPool.get()
        if (pool == null) {
            pool = LazyArgPool()
            threadLocalPool.set(pool)
        }
        return pool
    }

    fun evaluate(node: JsonLogicNode, data: Any?): Any? {
        return when (node) {
            is JsonLogicNull -> null
            is JsonLogicBoolean -> node.value
            is JsonLogicNumber -> {
                val d = node.value
                if (d == d.toLong().toDouble() && d >= Int.MIN_VALUE && d <= Int.MAX_VALUE) {
                    d.toInt()
                } else if (d == d.toLong().toDouble()) {
                    d.toLong()
                } else {
                    d
                }
            }
            is JsonLogicString -> node.value
            is JsonLogicArray -> node.value.map { evaluate(it, data) }
            is JsonLogicMap -> node.value.mapValues { evaluate(it.value, data) }
            is JsonLogicOperation -> evaluateOperation(node, data)
        }
    }

    private fun evaluateOperation(operation: JsonLogicOperation, data: Any?): Any? {
        val expression = expressions[operation.operator]
            ?: throw JsonLogicUnknownOperatorException(operation.operator)

        val rawArgs = operation.arguments.value

        return when (expression) {
            is LazyEvaluatedArgumentsExpression -> {
                val pool = getPool()
                val lazyArgs = pool.acquire(rawArgs.size)
                try {
                    for (i in rawArgs.indices) {
                        lazyArgs[i].reset(rawArgs[i], data, this)
                    }
                    expression.evaluate(lazyArgs, data, this)
                } finally {
                    pool.release(lazyArgs)
                }
            }
            else -> {
                val evaluatedArgs = rawArgs.map { evaluate(it, data) }
                expression.evaluate(evaluatedArgs, data, this)
            }
        }
    }

    fun evaluateTruthy(node: JsonLogicNode, data: Any?): Boolean {
        return TypeCoercion.isTruthy(evaluate(node, data))
    }
}

internal class LazyArgPool {
    private val pool = ArrayDeque<MutableList<LazyArg>>()

    companion object {
        private const val MAX_POOLED_LISTS = 4
    }

    fun acquire(size: Int): MutableList<LazyArg> {
        val list = pool.removeLastOrNull() ?: mutableListOf()
        while (list.size < size) {
            list.add(LazyArg())
        }
        if (list.size > size) {
            list.subList(size, list.size).clear()
        }
        return list
    }

    fun release(list: MutableList<LazyArg>) {
        for (arg in list) {
            arg.clear()
        }
        if (pool.size < MAX_POOLED_LISTS) {
            pool.addLast(list)
        }
    }

    fun clear() {
        for (list in pool) {
            for (arg in list) {
                arg.clear()
            }
            list.clear()
        }
        pool.clear()
    }
}

class LazyArg internal constructor() {
    private var node: JsonLogicNode? = null
    private var data: Any? = null
    private var evaluator: JsonLogicEvaluator? = null
    private var evaluated = false
    private var cachedValue: Any? = null

    internal fun reset(node: JsonLogicNode, data: Any?, evaluator: JsonLogicEvaluator) {
        this.node = node
        this.data = data
        this.evaluator = evaluator
        this.evaluated = false
        this.cachedValue = null
    }

    internal fun clear() {
        this.node = null
        this.data = null
        this.evaluator = null
        this.cachedValue = null
        this.evaluated = false
    }

    val astNode: JsonLogicNode
        get() = node ?: throw IllegalStateException("LazyArg not initialized: call reset() first")

    fun evaluate(): Any? {
        if (!evaluated) {
            val eval = evaluator ?: throw IllegalStateException("LazyArg not initialized: call reset() first")
            val n = node ?: throw IllegalStateException("LazyArg not initialized: call reset() first")
            cachedValue = eval.evaluate(n, data)
            evaluated = true
        }
        return cachedValue
    }

    fun evaluateWith(newData: Any?): Any? {
        val eval = evaluator ?: throw IllegalStateException("LazyArg not initialized: call reset() first")
        val n = node ?: throw IllegalStateException("LazyArg not initialized: call reset() first")
        return eval.evaluate(n, newData)
    }
}
