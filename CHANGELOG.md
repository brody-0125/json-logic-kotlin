# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-01-08

### Changed
- Replaced `println` with SLF4J logging for proper logging abstraction
- Added SLF4J API as a runtime dependency
- Added Logback Classic as a test dependency for logging during tests

### Added
- `logback-test.xml` configuration for test logging output
- DEBUG level logging for debugging JsonLogic execution:
  - `JsonLogic`: Cache hit/miss, custom operation registration
  - `JsonLogicParser`: Parse timing and results
  - `JsonLogicEvaluator`: Operation execution and results
  - `LogExpression`: JsonLogic `log` operator output

## [1.0.0] - 2026-01-07

### Added
- Initial release of json-logic-kotlin
- Pure Kotlin implementation of [JsonLogic](https://jsonlogic.com/)
- Thread-safe API with LRU parse cache (Caffeine)
- Support for custom operations via `addOperation()`

#### Core Operations
- **Data Access**: `var`, `missing`, `missing_some`
- **Logic**: `if`, `==`, `===`, `!=`, `!==`, `!`, `!!`, `and`, `or`
- **Numeric**: `+`, `-`, `*`, `/`, `%`, `<`, `<=`, `>`, `>=`, `min`, `max`
- **Array**: `map`, `filter`, `reduce`, `all`, `some`, `none`, `merge`, `in`
- **String**: `cat`, `substr`, `in`
- **Miscellaneous**: `log`

#### Extended Operations (non-standard)
- `preserve`: Returns value without evaluation

#### Architecture
- Three-layer design: AST, Evaluator, Expressions
- `JsonLogicParser` for JSON to AST conversion
- `JsonLogicEvaluator` with scope stack for nested contexts
- Pre-evaluated and lazy-evaluated expression base classes
- JavaScript-like type coercion utilities
