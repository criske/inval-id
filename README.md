[![codecov](https://codecov.io/gh/criske/inval-id/branch/main/graph/badge.svg?token=YFSEQZ2LQ2)](https://codecov.io/gh/criske/inval-id)
# inval-id

Simple and lightweight library for input validation written in Kotlin. Core concept is reusable and composable validation rules.

#### Basic usasge

```kotlin
val notBlankRule = Validation<String> { input ->
   errorOnFail("Input must not be blank") { input.isBlank() } 
}
val passwordRule = Validation<String> { input ->
   errorOnFail("Password must have length of at least 8") { input.length < 8 } 
}

fun signUp(username: String, password: String): Result<Unit> =
   Input.merge(
      notBlankRule validates username withId "username",
      ComposedValidation(notBlankRule, passwordRule) validates password withId "password"
   ).flatMap {
      signUpService(username, password)
   }
fun signUpService(username: String, password: String): Result<Unit> = Result.success(Unit)
```
An input can support multiple validations. Not the that order matters: `ValidationException` will be thrown at first failed rule.

Failing a validation results in a `Result.failure` that wraps a `ValidationException` which also contains a list on constraint violations in format of
`id, message`.

#### Out-of-the-box rules offered by inval-id

These rules loosely follow [javax.validation.constraints](https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-frame.html):

- AssertTrue
- AssertFalse
- Email
- Digits
- Future
- FutureOrPresent
- NotBlank
- NotEmpty
- Max
- Min
- MinMax
- Negative
- NegativeOrZero
- Pattern
- Past
- PastOrPresent
- Positive
- PositiveOrZero
- Size

For usage see [unit tests](https://github.com/criske/inval-id/blob/main/src/test/kotlin/pcf/crskdev/inval/id/RulesTest.kt)

#### Object validation

```kotlin
data class Account(val email: String, val password: String, val info: Info)
data class Info(val phone: String, val about: String, val address: String)
val accountRule = ObjectValidation<Account> { account ->
   ComposedValidation(Rules.NotBlank(), Rules.Email()) validates account.email withId "email"
   ObjectValidation<Info> { info ->
      Rules.NotBlank() validates info.address withId "address"
      Rules.NotBlank() validates info.phone withId "phone"
   } validates account.info withId "info"
 }
```

#### Creating a validation input
There are two ways:
```kotlin
 val input = Input("email".toId(), email, Rules.NotBlank(), Rules.Email())
 val input = ComposedValidation(Rules.NotBlank(), Rules.Email()) validates email withId "email"
```
Bypassing validation:
```kotlin
val input = Input.byPass(email)
```
