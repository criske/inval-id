![Release](https://jitpack.io/v/criske/inval-id.svg)
![status](https://github.com/criske/inval-id/actions/workflows/main.yml/badge.svg)
[![codecov](https://codecov.io/gh/criske/inval-id/branch/main/graph/badge.svg?token=YFSEQZ2LQ2)](https://codecov.io/gh/criske/inval-id)
# inval-id

Simple and lightweight library for input validation written in Kotlin. Core concept is reusable and composable validation rules.

#### Install
```gradle
repositories {
  maven { url 'https://jitpack.io' }
}
dependencies {
  implementation 'com.github.criske:inval-id:1.0'
}
```


#### Creating a validation input
There are two ways:
```kotlin
 val email = "foo@example.com"
 val input: Input<String> = Input("email".toId(), email, Rules.NotBlank(), Rules.Email())
 //or
 val input = ComposedValidation(Rules.NotBlank(), Rules.Email()) validates email withId "email"
```
Note that the `Id` could be any value, and is up to user to ensure this is unique.
This id will appear in `ValidationException` errors (constraint violations) `Result` 
and helps to identify which input failed.

#### Creating a validation rule 
Creating a validation rule is simple. Just use `Validation<T>` builder function. 
```kotlin
val rule = Validation<String> { value ->
    if(value.isBlank()){
        error("Must not be blank")
    }
}
//or
val rule = Validation<String> {
    errorOnFail("Must not be blank") { it.isBlank() }
}
val inputA = rule validates "foo" withId 1
val inputB = rule validates "foo" withId 2
```
Under the hood this creates the actual validation rule a low level function having the signature `(T, Id, ValidationExceptionProvider) -> Result<T>`
(aliased as `Validation<T>`).

This way will create the "illusion" of using objects, but in fact these are just functions.

```kotlin
val rule: Validation<String> = Validation<String>{}
```

The low level function can be used to create rules too, but the builder is more convenient.

#### Composing validation rules

```kotlin
val composed = ComposedValidation<String>(rule1, rule2, rule3, rule4)
val input = composed validates "foo" withId 1
```

#### Running validation on an input
```kotlin
val result: Result<String> = input
        .runValidation()
        .onSuccess { value -> println("Email $value is valid")}
        .onFailure { throwable -> println((throwable as ValidationException).violations)}
//or
val result: Result<String> = input()
```
Validation is applied when invoking the input.

An input can support multiple validations. Note the that order matters: validation will stop at first failed rule. 

Failing a validation will result in a `Result.failure` that wraps a `ValidationException`. This contains a list of 
constraint violations in format of `id, message`. Most of the time this list has the violation of
the first failed rule. Multiple violations can be found only in the case of [merging inputs](#merging-inputs) validations.

#### Object validation
Validations rule can be applied to objects too:

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

#### Merging inputs
```kotlin
val merged = inputA + inputB
//or
val merged = inputA.merge(inputB)
```
If the running validation on merged is a successful result, then input values for A and B will be wrapped in a list.

In the case of merged inputs, validations run for all inputs, so for example if inputA fails, the validations will
not stop and will check the inputB too (acts like a form validation).

When merging an input for which we don't want to apply a validation rule, then `Input.byPass` is used:
```kotlin
val merged = inputA + Input.byPass(myValue)
```

#### Adapting an input type to a rule type.
Sometimes we might need to adapt an input type to an existing validation rule that doesn't support that type.

Scenario: Have a password input as a `CharArray`.
For example, in order to test its strength with inval-id `Rules.Pattern`, 
we need to transform the value to a `CharSequence`.
If password is being validated by multiple array rules, this will break the composition though,
since `Rule.Pattern` only supports `CharSequence` as input values. 
So we need to transform that `CharArray` input value to a `CharSequence` before `Rules.Pattern` is applied.

```kotlin
 val adaptedRule: Validation<CharArray> = Pattern()("^.{8,}$").adapt { CharBuffer.wrap(it) }

 val input = ComposedValidation(NotEmpty(), adaptedRule) validates "abcd1234".toCharArray() withId 1
 val result = input().onSuccess { println("Password is valid")}
```

#### Out-of-the-box rules offered by inval-id

These rules loosely follow [javax.validation.constraints](https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-frame.html):

- AssertTrue
- AssertFalse
- Email
- Digits
  - DigitsInt
  - DigitsStr
  - Fractions
  - FractionsStr
  - Integers
  - IntegersStr
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

