# json-logic-kotlin

[JsonLogic](https://jsonlogic.com/)의 순수 Kotlin 구현체 - JSON으로 이식 가능한 로직 규칙을 작성하는 방법입니다.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9%2B-blue.svg)](https://kotlinlang.org/)
[![100% Compatibility](https://img.shields.io/badge/JsonLogic-100%25%20Compatible-green.svg)](https://jsonlogic.com/)

## 개요

**주요 특징:**
- 순수 Kotlin 구현 (JavaScript 엔진 불필요)
- 스레드 안전 및 재사용 가능
- 32개의 모든 표준 JsonLogic 연산 지원
- 커스텀 연산으로 확장 가능
- 공식 JsonLogic 테스트 스위트 100% 통과 (405개 테스트)

## 설치

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

## 사용법

### 기본 예제

```kotlin
import io.github.brodykim.jsonlogic.JsonLogic

val jsonLogic = JsonLogic()

// 간단한 비교
val result = jsonLogic.apply("""{"==": [1, 1]}""", null)
// result: true

// 복합 로직
val result2 = jsonLogic.apply(
    """{"and": [{">": [3, 1]}, {"<": [1, 3]}]}""",
    null
)
// result2: true
```

### 데이터 기반 규칙

```kotlin
val jsonLogic = JsonLogic()

// var로 데이터 접근
val result = jsonLogic.apply(
    """{"var": "user.name"}""",
    mapOf("user" to mapOf("name" to "홍길동"))
)
// result: "홍길동"

// 데이터를 활용한 조건부 로직
val rule = """
{
    "if": [
        {"<": [{"var": "temp"}, 0]}, "영하",
        {"<": [{"var": "temp"}, 20]}, "쌀쌀함",
        "더움"
    ]
}
"""
val result2 = jsonLogic.apply(rule, mapOf("temp" to 25))
// result2: "더움"
```

### 배열 연산

```kotlin
val jsonLogic = JsonLogic()

// Map: 각 요소를 2배로
val result = jsonLogic.apply(
    """{"map": [{"var": "numbers"}, {"*": [{"var": ""}, 2]}]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result: [2, 4, 6, 8, 10]

// Filter: 2보다 큰 값만 유지
val result2 = jsonLogic.apply(
    """{"filter": [{"var": "numbers"}, {">": [{"var": ""}, 2]}]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result2: [3, 4, 5]

// Reduce: 모든 값의 합계
val result3 = jsonLogic.apply(
    """{"reduce": [{"var": "numbers"}, {"+": [{"var": "current"}, {"var": "accumulator"}]}, 0]}""",
    mapOf("numbers" to listOf(1, 2, 3, 4, 5))
)
// result3: 15
```

### 커스텀 연산

```kotlin
val jsonLogic = JsonLogic()

// 커스텀 연산 추가
jsonLogic.addOperation("double") { args, _, _ ->
    (args[0] as? Number)?.toDouble()?.times(2)
}

val result = jsonLogic.apply("""{"double": 21}""", null)
// result: 42

// 인사 연산 추가
jsonLogic.addOperation("greet") { args, _, _ ->
    "안녕하세요, ${args[0]}님!"
}

val result2 = jsonLogic.apply(
    """{"greet": [{"var": "name"}]}""",
    mapOf("name" to "홍길동")
)
// result2: "안녕하세요, 홍길동님!"
```

### Truthy 평가

```kotlin
// JsonLogic 규칙에 따라 값이 truthy인지 확인
JsonLogic.truthy(1)        // true
JsonLogic.truthy(0)        // false
JsonLogic.truthy("")       // false
JsonLogic.truthy("hello")  // true
JsonLogic.truthy(listOf<Any>()) // false (빈 배열은 falsy)
JsonLogic.truthy(null)     // false
```

## 지원 연산

### 데이터 접근
| 연산 | 설명 | 예제 |
|------|------|------|
| `var` | 데이터 값 접근 | `{"var": "property"}` |
| `missing` | 누락된 키 가져오기 | `{"missing": ["a", "b"]}` |
| `missing_some` | 최소 키 확인 | `{"missing_some": [1, ["a", "b"]]}` |

### 로직 & 불리언
| 연산 | 설명 | 예제 |
|------|------|------|
| `if`, `?:` | 조건문 | `{"if": [cond, then, else]}` |
| `==` | 느슨한 동등 | `{"==": [1, "1"]}` |
| `===` | 엄격한 동등 | `{"===": [1, 1]}` |
| `!=` | 느슨한 부등 | `{"!=": [1, 2]}` |
| `!==` | 엄격한 부등 | `{"!==": [1, "1"]}` |
| `!` | 부정 | `{"!": false}` |
| `!!` | 이중 부정 | `{"!!": 1}` |
| `or` | 논리 OR | `{"or": [false, true]}` |
| `and` | 논리 AND | `{"and": [true, true]}` |

### 숫자
| 연산 | 설명 | 예제 |
|------|------|------|
| `>`, `>=`, `<`, `<=` | 비교 | `{">": [2, 1]}` |
| `max`, `min` | 최대/최소값 | `{"max": [1, 2, 3]}` |
| `+`, `-`, `*`, `/` | 산술 연산 | `{"+": [1, 2]}` |
| `%` | 나머지 | `{"%": [5, 2]}` |

### 배열
| 연산 | 설명 | 예제 |
|------|------|------|
| `map` | 요소 변환 | `{"map": [arr, logic]}` |
| `filter` | 요소 필터링 | `{"filter": [arr, cond]}` |
| `reduce` | 값으로 축소 | `{"reduce": [arr, logic, init]}` |
| `all`, `none`, `some` | 요소 테스트 | `{"all": [arr, cond]}` |
| `merge` | 배열 병합 | `{"merge": [[1], [2]]}` |
| `in` | 포함 여부 테스트 | `{"in": [val, arr]}` |

### 문자열
| 연산 | 설명 | 예제 |
|------|------|------|
| `cat` | 연결 | `{"cat": ["a", "b"]}` |
| `substr` | 부분 문자열 | `{"substr": ["hello", 1, 3]}` |

### 기타
| 연산 | 설명 | 예제 |
|------|------|------|
| `log` | 디버그 출력 | `{"log": "message"}` |

## 호환성

이 구현체는 [jsonlogic.com/tests.json](https://jsonlogic.com/tests.json)의 **공식 JsonLogic 테스트 스위트 100%를 통과**합니다.

| 테스트 카테고리 | 테스트 수 | 상태 |
|----------------|----------|------|
| 공식 호환성 테스트 | 278 | 통과 |
| 추가 단위 테스트 | 127 | 통과 |
| **합계** | **405** | **100%** |

## 관련 프로젝트

- [json-logic-js](https://github.com/jwadhams/json-logic-js) - 원본 JavaScript 구현체
- [json-logic-java](https://github.com/jamsesso/json-logic-java) - Java 구현체
- [JsonLogic.com](https://jsonlogic.com/) - 공식 명세

## 라이선스

MIT 라이선스
