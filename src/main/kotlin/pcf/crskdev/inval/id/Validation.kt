/*
 * MIT License
 *
 * Copyright (c) 2021. Pela Cristian
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("unused")

package pcf.crskdev.inval.id

/**
 * Function alias that creates a validation rule based of the input value T.
 * It returns a [Result] with the input value on success or, on failure, a
 * [ValidationException] created with [ValidationExceptionProvider].
 */
typealias Validation<T> = (T, ValidationExceptionProvider) -> Result<T>

/**
 * De facto way to create a [Validation] rule.
 * ```
 * val email: Validation<String> = Validation {
 *      if (!it.matches("^(.+)@(.+)$".toRegex())) {
 *          error("\"$it\" is not a valid email address")
 *      }
 * }
 * ```
 * @param T input value type
 * @param block Lambda with receiver ValidationScope.
 * @receiver Provides the ValidationScope and the input value.
 * @return Validation rule.
 */
@Suppress("FunctionName")
fun <T> Validation(block: ValidationScope<T>.(T) -> Unit): Validation<T> = { input, error ->
    val scope = ValidationScope(input, error).apply { block(input) }
    if (scope.builder.isEmpty) {
        Result.success(input)
    } else {
        Result.failure(scope.builder.build())
    }
}

/**
 * Validation scope. used by Validation helper function, on the "block" param lambda with receiver.
 *
 * @property provider ValidationExceptionProvider
 * @constructor Create empty Validation scope
 */
class ValidationScope<T> internal constructor(
    private val input: T,
    private val provider: ValidationExceptionProvider
) {

    internal val builder = ValidationException.Builder()

    /**
     * Add message error to the final field ValidationException.
     *
     * @param message CharSequence.
     */
    fun error(message: CharSequence) {
        builder.add(provider(message))
    }

    /**
     * Add message error to the final field ValidationException when predicate fails.
     * Shortcut for:
     * ```
     * if(input fails) {
     *  error("Invalid input")
     * }
     * ```
     * @param message CharSequence.
     * @param predicate Predicate applied to input T.
     * @receiver
     */
    fun errorOnFail(message: CharSequence, predicate: (T) -> Boolean) {
        if (predicate(this.input)) {
            builder.add(provider(message))
        }
    }
}

/**
 * Creates a regex based validation.
 * ```
 * val email: Validation<String> = RegexValidation("^(.+)@(.+)$")("Not a valid email.")
 * ```
 * @param expression Regex expression.
 * @param options Regex options.
 * @return Custom message Validation.
 */
@Suppress("FunctionName")
fun RegexValidation(expression: String, options: Set<RegexOption> = emptySet()): CustomMessageValidation<CharSequence> {
    val regex = when {
        options.isEmpty() -> expression.toRegex()
        options.size == 1 -> expression.toRegex(options.first())
        else -> expression.toRegex(options)
    }
    return { message ->
        val validation: Validation<CharSequence> = Validation {
            if (!regex.matches(it)) {
                error(message)
            }
        }
        validation
    }
}

/**
 * Custom message validation.
 */
typealias CustomMessageValidation<T> = (CharSequence) -> Validation<T>

/**
 * Composes multiple validation rules into one rule.
 *
 * @param T input Type.
 * @param validations [Validation] rules.
 * @return Result.
 */
fun <T> ComposedValidation(vararg validations: Validation<T>): Validation<T> = { input, error ->
    validations.fold(Result.success(input)) { acc, curr ->
        acc.flatMap { curr(input, error) }
    }.onFailure { e ->
        if (e !is ValidationException)
            throw IllegalStateException(
                """
                   Input failure expected to be a ValidationException but was ${e.javaClass.simpleName}.
                   To avoid this kind error build your rule by using Validation helper function.
                """.trimIndent()

            )
    }
}

/**
 * Declarative validation.
 *
 * @param T Input type.
 * @param input Input value.
 * @return Function that will apply the [Id].
 */
infix fun <T> Validation<T>.validates(input: T): (Id) -> Input<T> = {
    Input(it, input, this)
}

/**
 * Extension for the Function that will apply the [Id].
 * Part of declarative validation chain. See [validates].
 *
 * @param T Input type.
 * @param id Id.
 * @return Input.
 */
infix fun <T> ((Id) -> Input<T>).withId(id: Id): Input<T> = this(id)

/**
 * Extension for the Function that will apply the [Id].
 * Part of declarative validation chain. See [validates].
 *
 * @param T Input type.
 * @param id Any value that will be converted to [Id].
 * @return Input.
 */
infix fun <T> ((Id) -> Input<T>).withId(id: Any): Input<T> = this(id.toId())
