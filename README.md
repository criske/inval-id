[![codecov](https://codecov.io/gh/criske/inval-id/branch/main/graph/badge.svg?token=YFSEQZ2LQ2)](https://codecov.io/gh/criske/inval-id)
# inval-id

Simple and lightweight library for input validation written in Kotlin. Core concept is reusable and composable validation rules.

#### Creating a validation input
There are two ways:
```kotlin
 val email = "foo@example.com"
 val input: Input<String> = Input("email".toId(), email, Rules.NotBlank(), Rules.Email())
 //or
 val input: Input<String> = ComposedValidation(Rules.NotBlank(), Rules.Email()) validates email withId "email"

 val result: Result<String> = input()
        .onSuccess { println("Email is valid")}
        .onFailure { throwable -> println((throwable as ValidationException).errors)}
```
Bypassing validation:
```kotlin
val input = Input.byPass(email)
```
Validation is applied when invoking the input
An input can support multiple validations. Note the that order matters: `ValidationException` will be thrown at first failed rule.

Failing a validation results in a `Result.failure` that wraps a `ValidationException` which also contains a list on constraint violations in format of
`id, message`.

#### Basic example

```kotlin
val notBlankRule = Validation<String> { input ->
   errorOnFail("Input must not be blank") { input.isBlank() } 
}
val passwordRule = Validation<String> { input ->
   errorOnFail("Password must have length of at least 8") { input.length < 8 } 
}

fun signUp(username: String, password: String): Result<Unit> =
   // Input.merge() merges the inputs, applies validations and returns a Result of Pair, Triple
   // based on the number of inputs
   // If inputs are larger than 3, use Input.mergeAny() the result is a List of input values.
   Input.merge(
      notBlankRule validates username withId "username",
      ComposedValidation(notBlankRule, passwordRule) validates password withId "password"
   ).flatMap {
      signUpService(username, password)
   }
fun signUpService(username: String, password: String): Result<Unit> = Result.success(Unit)
```

#### Adapting an input type to a rule type.
Sometimes we might need to adapt an input type to an existing validation rule that doesn't support that type.

Scenario: Have a password input as a CharArray.
For example, in order to test its strength with inval-id Rules.Pattern, 
we need to transform the value to a CharSequence.
If password is being validated by multiple array rules, this will break the composition
since Rule.Pattern only supports CharSequence as input. 
So we need to transform that CharArray input value to a CharSequence before Rules.Pattern is applied.

```kotlin
 val adaptedRule: Validation<CharArray> =
     Pattern { _, _ -> "Weak password: must at least 8 in length" }("^.{8,}$")
                .adapt { CharBuffer.wrap(it) }
 val input = ComposedValidation(NotEmpty(), adaptedRule) validates "abcd1234".toCharArray() withId 1
 val result = input().onSuccess { println("Password is valid")}
```

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
