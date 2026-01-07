# json-logic-kotlin

A pure Kotlin implementation of [JsonLogic](https://jsonlogic.com/) - a way to write portable logic rules as JSON.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9%2B-blue.svg)](https://kotlinlang.org/)
[![100% Compatibility](https://img.shields.io/badge/JsonLogic-100%25%20Compatible-green.svg)](https://jsonlogic.com/)

## Overview

**Key Features:**
- Pure Kotlin implementation (no JavaScript engine required)
- Thread-safe and reusable
- All 32 standard JsonLogic operations supported
- Extensible with custom operations
- Passes 100% of official JsonLogic test suite (405 tests)

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.brody-0125:json-logic-kotlin:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.brody-0125:json-logic-kotlin:1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.brody-0125</groupId>
    <artifactId>json-logic-kotlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Basic Example

```kotlin
import io.github.brodykim.jsonlogic.JsonLogic

val jsonLogic = JsonLogic()

// Simple comparison
val result = jsonLogic.apply("""{"==": [1, 1]}""", null)
// result: true

// Compound logic
val result2 = jsonLogic.apply(
    """{"and": [{">": [3, 1]}, {"<": [1, 3]}]}""",
    null
)
// result2: true
```

### Data-Driven Rules

```kotlin
val jsonLogic = JsonLogic()

// Access data with var
val result = jsonLogic.apply(
    """{"var": "user.name"}""",
    mapOf("user" to mapOf("name" to "John"))
)
// result: "John"

// Conditional logic with data
val rule = """
{
    "if": [
        {"<": [{"var": "temp"}, 0]}, "freezing",
        {"<": [{"var": "temp"}, 20]}, "cold",
        "hot"
    ]
}
"""
val result2 = jsonLogic.apply(rule, mapOf("temp" to 25))
// result2: "hot"
```

### Array Operations

```kotlin
val jsonLogic = JsonLogic()

// Map: Double each element
val result = jsonLogic.apply(
    """{"map": [{"var": "numbers"}, {"*": [{"var": ""}, 2]}]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result: [2, 4, 6, 8, 10]

// Filter: Keep values greater than 2
val result2 = jsonLogic.apply(
    """{"filter": [{"var": "numbers"}, {">": [{"var": ""}, 2]}]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result2: [3, 4, 5]

// Reduce: Sum all values
val result3 = jsonLogic.apply(
    """{"reduce": [{"var": "numbers"}, {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result3: 15
```

### Custom Operations

```kotlin
val jsonLogic = JsonLogic()

// Add a custom operation
jsonLogic.addOperation("double") { args, _, _ ->
    (args[0] as? Number)?.toDouble()?.times(2)
}

val result = jsonLogic.apply("""{"double": 21}""", null)
// result: 42

// Add a greeting operation
jsonLogic.addOperation("greet") { args, _, _ ->
    "Hello, ${args[0]}!"
}

val result2 = jsonLogic.apply(
    """{"greet": [{"var": "name"}]}""",
    mapOf("name" to "World")
)
// result2: "Hello, World!"
```

### Truthy Evaluation

```kotlin
// Check if a value is truthy according to JsonLogic rules
JsonLogic.truthy(1)        // true
JsonLogic.truthy(0)        // false
JsonLogic.truthy("")       // false
JsonLogic.truthy("hello")  // true
JsonLogic.truthy(listOf<Any>()) // false (empty arrays are falsy)
JsonLogic.truthy(null)     // false
```

## Supported Operations

### Data Access
| Operation | Description | Example |
|-----------|-------------|---------|
| `var` | Access data values | `{"var": "property"}` |
| `missing` | Get missing keys | `{"missing": ["a", "b"]}` |
| `missing_some` | Check minimum keys | `{"missing_some": [1, ["a", "b"]]}` |

### Logic & Boolean
| Operation | Description | Example |
|-----------|-------------|---------|
| `if`, `?:` | Conditional | `{"if": [cond, then, else]}` |
| `==` | Loose equality | `{"==": [1, "1"]}` |
| `===` | Strict equality | `{"===": [1, 1]}` |
| `!=` | Loose inequality | `{"!=": [1, 2]}` |
| `!==` | Strict inequality | `{"!==": [1, "1"]}` |
| `!` | Negation | `{"!": false}` |
| `!!` | Double negation | `{"!!": 1}` |
| `or` | Logical OR | `{"or": [false, true]}` |
| `and` | Logical AND | `{"and": [true, true]}` |

### Numeric
| Operation | Description | Example |
|-----------|-------------|---------|
| `>`, `>=`, `<`, `<=` | Comparison | `{">": [2, 1]}` |
| `max`, `min` | Find extremes | `{"max": [1, 2, 3]}` |
| `+`, `-`, `*`, `/` | Arithmetic | `{"+": [1, 2]}` |
| `%` | Modulo | `{"%": [5, 2]}` |

### Array
| Operation | Description | Example |
|-----------|-------------|---------|
| `map` | Transform elements | `{"map": [arr, logic]}` |
| `filter` | Filter elements | `{"filter": [arr, cond]}` |
| `reduce` | Reduce to value | `{"reduce": [arr, logic, init]}` |
| `all`, `none`, `some` | Test elements | `{"all": [arr, cond]}` |
| `merge` | Combine arrays | `{"merge": [[1], [2]]}` |
| `in` | Membership test | `{"in": [val, arr]}` |

### String
| Operation | Description | Example |
|-----------|-------------|---------|
| `cat` | Concatenate | `{"cat": ["a", "b"]}` |
| `substr` | Substring | `{"substr": ["hello", 1, 3]}` |

### Miscellaneous
| Operation | Description | Example |
|-----------|-------------|---------|
| `log` | Debug output | `{"log": "message"}` |

## Compatibility

This implementation passes **100% of the official JsonLogic test suite** from [jsonlogic.com/tests.json](https://jsonlogic.com/tests.json).

| Test Category | Tests | Status |
|--------------|-------|--------|
| Official Compatibility Tests | 278 | Pass |
| Additional Unit Tests | 127 | Pass |
| **Total** | **405** | **100%** |

### compat-tables Validation

Additionally tested against [json-logic/compat-tables](https://github.com/json-logic/compat-tables) for cross-implementation compatibility:

| Metric | Value |
|--------|-------|
| Total Tests | 964 |
| Passed | 934 |
| Failed | 20 |
| Skipped | 10 |
| **Pass Rate** | **97.90%** |

> **Note:** Failed tests are edge cases in extended operations (scope climbing, lazy evaluation checks). All official JsonLogic operations pass 100%.

#### Suite Results

| Suite | Pass Rate | Notes |
|-------|-----------|-------|
| compatible | 100% | Official JsonLogic compatibility tests |
| array/* | 100% | all, filter, map, merge, none, some, reduce |
| string/* | 100% | cat, in, substr |
| control/* | 100% | if, and, or, not, !! |
| comparison/* | 100% | ==, ===, !=, !==, <, <=, >, >= |
| arithmetic/* | 100% | +, -, *, /, % |

Full compatibility report available at [`compat-tests/results/REPORT.md`](compat-tests/results/REPORT.md).

## Related Projects

- [json-logic-js](https://github.com/jwadhams/json-logic-js) - Original JavaScript implementation
- [json-logic-java](https://github.com/jamsesso/json-logic-java) - Java implementation
- [JsonLogic.com](https://jsonlogic.com/) - Official specification

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
