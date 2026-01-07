# JsonLogic Kotlin Implementation Specification

## Overview

This document defines the implementation specification for JsonLogic in Kotlin, based on the official specification at [jsonlogic.com](https://jsonlogic.com/).

## What is JsonLogic?

JsonLogic is a lightweight system for encoding rules as JSON data. It allows sharing logic between frontend and backend systems while maintaining a simple, declarative structure.

### Key Principles

1. **Single Operator Rule**: Each rule has exactly one operator as the key
2. **Values as Arrays**: Arguments are typically passed as arrays
3. **Composable**: Rules can be nested within other rules
4. **Data-Driven**: Rules can reference external data using the `var` operator
5. **No Side Effects**: Rules are purely functional (except `log` for debugging)

## Rule Structure

```json
{"operator": ["values", ...]}
```

Example:
```json
{"==": [1, 1]}
```

## Supported Operations

### Data Access

| Operator | Description | Example |
|----------|-------------|---------|
| `var` | Access data values | `{"var": "property"}` |
| `missing` | Get missing keys | `{"missing": ["a", "b"]}` |
| `missing_some` | Check minimum keys | `{"missing_some": [1, ["a", "b"]]}` |

#### var Operator

- Dot notation: `{"var": "a.b.c"}`
- Array index: `{"var": 1}` or `{"var": "items.0"}`
- Default value: `{"var": ["x", "default"]}`
- Entire data: `{"var": ""}`

### Logic & Boolean Operations

| Operator | Description | Example |
|----------|-------------|---------|
| `if`, `?:` | Conditional | `{"if": [cond, then, else]}` |
| `==` | Loose equality | `{"==": [1, "1"]}` → true |
| `===` | Strict equality | `{"===": [1, "1"]}` → false |
| `!=` | Loose inequality | `{"!=": [1, 2]}` → true |
| `!==` | Strict inequality | `{"!==": [1, "1"]}` → true |
| `!` | Negation | `{"!": false}` → true |
| `!!` | Double negation | `{"!!": 1}` → true |
| `or` | Logical OR | `{"or": [false, 1]}` → 1 |
| `and` | Logical AND | `{"and": [1, 2]}` → 2 |

#### Truthy/Falsy Rules

Falsy values:
- `null`
- `false`
- `0`, `0.0`, `NaN`
- `""` (empty string)
- `[]` (empty array)

Everything else is truthy.

### Numeric Operations

| Operator | Description | Example |
|----------|-------------|---------|
| `>` | Greater than | `{">": [2, 1]}` → true |
| `>=` | Greater or equal | `{">=": [1, 1]}` → true |
| `<` | Less than | `{"<": [1, 2]}` → true |
| `<=` | Less or equal | `{"<=": [1, 2]}` → true |
| `max` | Maximum | `{"max": [1, 2, 3]}` → 3 |
| `min` | Minimum | `{"min": [1, 2, 3]}` → 1 |
| `+` | Addition | `{"+": [1, 2]}` → 3 |
| `-` | Subtraction | `{"-": [3, 1]}` → 2 |
| `*` | Multiplication | `{"*": [2, 3]}` → 6 |
| `/` | Division | `{"/": [6, 2]}` → 3 |
| `%` | Modulo | `{"%": [5, 2]}` → 1 |

#### Between Tests

Comparison operators support 3 arguments for "between" tests:
```json
{"<": [1, 2, 3]}   // 1 < 2 < 3 → true (exclusive)
{"<=": [1, 2, 3]}  // 1 <= 2 <= 3 → true (inclusive)
```

### Array Operations

| Operator | Description | Example |
|----------|-------------|---------|
| `map` | Transform elements | `{"map": [arr, transform]}` |
| `filter` | Filter elements | `{"filter": [arr, condition]}` |
| `reduce` | Reduce to value | `{"reduce": [arr, logic, initial]}` |
| `all` | All match | `{"all": [arr, condition]}` |
| `none` | None match | `{"none": [arr, condition]}` |
| `some` | Some match | `{"some": [arr, condition]}` |
| `merge` | Combine arrays | `{"merge": [[1], [2]]}` → [1, 2] |
| `in` | Array/string membership | `{"in": [val, arr]}` |

#### Map/Filter/Reduce Context

Within these operations, `{"var": ""}` refers to the current element:
```json
{"map": [[1, 2, 3], {"*": [{"var": ""}, 2]}]}
// → [2, 4, 6]
```

For reduce, use `current` and `accumulator`:
```json
{"reduce": [[1, 2, 3], {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}
// → 6
```

### String Operations

| Operator | Description | Example |
|----------|-------------|---------|
| `cat` | Concatenate | `{"cat": ["a", "b"]}` → "ab" |
| `substr` | Substring | `{"substr": ["hello", 1, 3]}` → "ell" |
| `in` | Substring test | `{"in": ["lo", "hello"]}` → true |

#### substr Parameters

- `start`: Start position (negative = from end)
- `length`: Optional length (negative = stop before end)

### Miscellaneous

| Operator | Description | Example |
|----------|-------------|---------|
| `log` | Debug output | `{"log": "message"}` |

## Type Coercion

### Loose Equality (`==`, `!=`)

- Number to String: String converted to number
- Boolean to anything: Boolean converted to number (true=1, false=0)
- Null equals only null

### Strict Equality (`===`, `!==`)

- Types must match exactly
- Exception: Different numeric types (Int, Long, Double) compare by value

## Implementation Architecture

### Project Structure

```
src/main/kotlin/io/github/brodykim/jsonlogic/
├── JsonLogic.kt                    # Main entry point
├── JsonLogicException.kt           # Custom exceptions
├── ast/                            # Abstract Syntax Tree
│   ├── JsonLogicNode.kt            # Sealed interface for AST nodes
│   └── JsonLogicParser.kt          # JSON to AST parser
├── evaluator/
│   ├── JsonLogicEvaluator.kt       # Core evaluation engine
│   ├── JsonLogicExpression.kt      # Expression interface
│   └── expressions/                # Operation implementations
│       ├── DataAccessExpressions.kt
│       ├── LogicExpressions.kt
│       ├── NumericExpressions.kt
│       ├── ArrayExpressions.kt
│       ├── StringExpressions.kt
│       ├── MiscExpressions.kt
│       └── ExtendedExpressions.kt
└── utils/
    ├── TypeCoercion.kt             # Type coercion utilities
    └── CollectionUtils.kt          # Collection utilities
```

### AST Node Types

```kotlin
sealed interface JsonLogicNode
data object JsonLogicNull : JsonLogicNode
data class JsonLogicBoolean(val value: Boolean) : JsonLogicNode
data class JsonLogicNumber(val value: Double) : JsonLogicNode
data class JsonLogicString(val value: String) : JsonLogicNode
data class JsonLogicArray(val value: List<JsonLogicNode>) : JsonLogicNode
data class JsonLogicMap(val value: Map<String, JsonLogicNode>) : JsonLogicNode
data class JsonLogicOperation(val operator: String, val arguments: JsonLogicArray) : JsonLogicNode
```

### Expression Types

1. **PreEvaluatedArgumentsExpression**: Arguments evaluated before operation
2. **LazyEvaluatedArgumentsExpression**: Arguments evaluated on demand (for short-circuit logic)

## Usage

### Basic Usage

```kotlin
val jsonLogic = JsonLogic()

// Simple rule
val result = jsonLogic.apply("""{"==": [1, 1]}""", null)
// result: true

// With data
val result2 = jsonLogic.apply(
    """{"==": [{"var": "temp"}, "hot"]}""",
    mapOf("temp" to "hot")
)
// result2: true
```

### Custom Operations

```kotlin
val jsonLogic = JsonLogic()

jsonLogic.addOperation("double") { args, _, _ ->
    (args[0] as? Number)?.toDouble()?.times(2)
}

val result = jsonLogic.apply("""{"double": 21}""", null)
// result: 42
```

### Thread Safety

The `JsonLogic` class is thread-safe and can be shared across threads.

## Compatibility

This implementation passes 100% of the official JsonLogic test suite from [jsonlogic.com/tests.json](https://jsonlogic.com/tests.json).

### Test Coverage

- 278 official compatibility tests
- 127 additional unit tests
- **405 total tests, 100% passing**

## Dependencies

- Kotlin 1.9+
- kotlinx.serialization-json 1.6.2
- JUnit 5 (for testing)
