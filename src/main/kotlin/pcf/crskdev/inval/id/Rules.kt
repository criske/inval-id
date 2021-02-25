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

@file:Suppress("FunctionName")

package pcf.crskdev.inval.id

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Built-in validation rules offered by inval-id that
 * loosely follow [javax.validation.constraints](https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-frame.html)
 *
 */
object Rules {

    /**
     * The value of the field or property must be false.
     *
     * @param message Message.
     * @return Validation<Boolean>
     */
    fun AssertTrue(message: String = "Boolean flag must be true"): Validation<Boolean> = Validation { input ->
        errorOnFail(message) { !input }
    }

    /**
     * The value of the field or property must be false.
     *
     * @param message Message.
     * @return Validation<Boolean>
     */
    fun AssertFalse(message: String = "Boolean flag must be false"): Validation<Boolean> = Validation { input ->
        errorOnFail(message) { input }
    }

    /**
     * The value of the field or property must contain at least one non-white space character.
     *
     * @param message Message.
     * @return Validation<CharSequence>
     */
    fun NotBlank(message: String = "Field or property required"): Validation<CharSequence> =
        Validation { input -> errorOnFail(message) { input.isBlank() } }

    /**
     * The value of the field or property must not be empty.
     * The length of the characters or array, and the size of a collection or map are evaluated.
     *
     * Supported types are:
     * - CharSequence (length of character sequence is evaluated)
     * - Collection (collection size is evaluated)
     * - Map (map size is evaluated)
     * - Array (array length is evaluated)
     *
     * @param message Message.
     * @return Validation<CharSequence>
     */
    fun <T> NotEmpty(message: String = "Field or property required"): Validation<T> =
        Validation { input ->
            errorOnFail(message) { sizeOf(input) == 0 }
        }

    /**
     * The value of the field or property must be a [Number] value lower than or equal
     * to the [Number] in the value element.
     *
     * Returns _(T) -> Validation<T>_ lambda, where T param is the maximum boundary [Number] against which
     * input will be tested.
     *
     * Example:
     *
     * _Max<Float>(scale = 2.places())(10.12f) validates 10.118654f withId 1_
     * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the max allowed)
     *
     * _Max<Int>()(10) validates 19 withId 1_ (fails because min is _10_ and input is _19_)
     *
     * @param T [Number] type.
     * @param messageProvider Custom message lambda. Takes the input and max as args.
     * @param scale [MathContext] approximation scale applicable for floats and doubles inputs decimal places.
     * otherwise for integers will be ignored.
     * @return (T) -> Validation<T>.
     */
    fun <T : Number> Max(
        scale: MathContext = MathContext.UNLIMITED,
        messageProvider: (T, T) -> String = { input, max -> "Input $input must be at most $max." }
    ): (T) -> Validation<T> = { max ->
        Validation { input ->
            val predicate: (T) -> Boolean = {
                input
                    .toBigDecimalInternal()
                    .setScale(scale.precision, scale.roundingMode) > max.toBigDecimalInternal()
                    .setScale(scale.precision, scale.roundingMode)
            }
            errorOnFail(messageProvider(input, max), predicate)
        }
    }

    /**
     * The value of the field or property must be a [Number] value larger than or equal
     * to the [Number] in the value element.         *
     * Returns _(T) -> Validation<T>_ lambda, where T param is the minimum boundary [Number] against which
     * input will be tested.
     *
     * Example:
     *
     * _Min<Float>(scale = 2.places())(10.12f) validates 10.118654f withId 1_
     * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the min allowed)
     *
     * _Min<Int>()(10) validates 9 withId 1_ (fails because min is _10_ and input is _9_)
     *
     * @param T [Number] type.
     * @param messageProvider Custom message lambda. Takes the input and min as args.
     * @param scale [MathContext] approximation scale applicable for floats and doubles inputs decimal places,
     * otherwise for integers will be ignored.
     * @return (T) -> Validation<T>.
     */
    fun <T : Number> Min(
        scale: MathContext = MathContext.UNLIMITED,
        messageProvider: (T, T) -> String = { input, min -> "Input $input must be at least $min." }
    ): (T) -> Validation<T> = { min ->
        Validation { input ->
            val predicate: (T) -> Boolean = {
                input
                    .toBigDecimalInternal()
                    .setScale(scale.precision, scale.roundingMode) < min.toBigDecimalInternal()
                    .setScale(scale.precision, scale.roundingMode)
            }
            errorOnFail(messageProvider(input, min), predicate)
        }
    }

    /**
     * Interval Rule, that uses internally [Min] and [Max] rules.
     *
     * @param T [Number] type.
     * @param messageProvider Message on fail. Lambda take Input, Min and Max values as args.
     * @param scale Decimal places scaling. See [Min] and [Max]
     * @return Lambda that takes [Min] Number and [Max] Number as params and returns a Validation.
     */
    fun <T : Number> MinMax(
        scale: MathContext = MathContext.UNLIMITED,
        messageProvider: (T, T, T) -> String = { input, min, max -> "$input must be between [$min, $max]" }
    ): (T, T) -> Validation<T> = { min, max ->
        ComposedValidation(
            Min<T>(scale) { input, _ -> messageProvider(input, min, max) }(min),
            Max<T>(scale) { input, _ -> messageProvider(input, min, max) }(max)
        )
    }

    /**
     * The value of the field or property must be a number within a specified range. The integer element specifies
     * the maximum integral digits for the number, and the fraction element specifies the maximum fractional
     * digits for the number.
     *
     * Example:
     *
     * _Digits<Double>()(3, 2) validates 120,32 withId 1_ passes
     *
     * _Digits<Double>()(3, 2) validates 12,32 withId 1_ fails
     *
     * See also : [DigitsInt], [DigitsStr]
     *
     * @param T Number Type.
     * @param messageProvider Message provider on fail.
     * @receiver Takes Input, Digits and Fractions as args and returns the message.
     * @return `(Int, Int) -> Validation` that takes Digits and Fractions as args.
     */
    fun <T : Number> Digits(
        messageProvider: (T, Int, Int) -> String = { input, integers, fractions -> "$input number must have $integers digits and $fractions fractions" }
    ): (Int, Int) -> Validation<T> = { integers, fractions ->
        Validation { input ->
            errorOnFail(messageProvider(input, integers, fractions)) {
                val inputBd = input.toBigDecimalInternal()
                abs(inputBd.precision() - inputBd.scale()) != integers || inputBd.scale() != fractions
            }
        }
    }

    /**
     * The value of the field or property must be a number within a specified range. The integer element specifies
     * the maximum integral digits for the number, and the fraction element specifies the maximum fractional
     * digits for the number.
     *
     * It assumes that that the string input is a number representation.
     *
     * This rule is useful when it applies to numbers that need to retain the fractional trailing zeros.
     *
     * Example:
     *
     * _DigitsStr()(3, 2) validates "120.30" withId 1_ passes
     *
     * but
     *
     * _Digits<Double>()(3, 2) validates 120.30 withId 1_ fails because the trailing zero is ignored and is interpreted
     * as having one decimal.
     *
     * See also : [Digits],  [DigitsInt]
     *
     * @param messageProvider Message provider on fail.
     * @receiver Takes Input, Digits and Fractions as args and returns the message.
     * @return `(Int, Int) -> Validation` that takes Digits and Fractions as args.
     */
    fun DigitsStr(
        messageProvider: (String, Int, Int) -> String = { input, integers, fractions -> "$input number must have $integers digits and $fractions fractions" }
    ): (Int, Int) -> Validation<String> = { integers, fractions ->
        Validation { input ->
            errorOnFail(messageProvider(input, integers, fractions)) {
                val inputBd = BigDecimal(input)
                abs(inputBd.precision() - inputBd.scale()) != integers || inputBd.scale() != fractions
            }
        }
    }

    /**
     * Convenience for `Digits<Int>(digits, 0)`.
     *
     * The value of the field or property must be an int number within a specified range. The integer element specifies
     * the maximum integral digits for the number.
     *
     * Example:
     *
     * _DigitsInt()(3) validates 120 withId 1_ passes
     *
     * _DigitsInt()(3) validates 12 withId 1_ fails
     *
     * @param messageProvider Message provider on fail.
     * @receiver Takes Input and Digits as args and returns the message.
     * @return `(Int -> Validation` that takes Digits as arg.
     */
    fun DigitsInt(
        messageProvider: (Int, Int) -> String = { input, integers -> "$input number must have $integers digits" }
    ): (Int) -> Validation<Int> = { integers ->
        Digits<Int> { input, _, _ -> messageProvider(input, integers) }(integers, 0)
    }

    /**
     * Handy extension to create a [MathContext] used by [Min] and [Max] for decimal scaling when dealing with
     * fraction inputs.
     *
     * Receiver is the number of decimal places after scaling will kick in.
     *
     * Example:
     *
     * "3.places(RoundingMode.HALF_EVEN)"
     *
     * @param roundingMode [RoundingMode] strategy.
     */
    fun Int.places(roundingMode: RoundingMode = RoundingMode.HALF_UP) = MathContext(this, roundingMode)

    /**
     * The size of the field or property is evaluated and must match the specified boundaries.
     *
     * If the field or property is a String, the size of the string is evaluated.
     *
     * If the field or property is a Collection, the size of the Collection is evaluated.
     *
     * If the field or property is a Map, the size of the Map is evaluated.
     *
     * If the field or property is an array, the size of the array is evaluated.
     *
     * Use one of the optional max or min elements to specify the boundaries.
     *
     * @param T type.
     * @param messageProvider Message provider on fail.
     * @receiver Takes Input, Min, Max as args and returns the message.
     * @return `(Int, Int) -> Validation<T>` where Min and Max are args.
     */
    fun <T> Size(
        messageProvider: (T, Int, Int) -> String = { input, min, max -> "$input size be between [$min, $max]" }
    ): (Int, Int) -> Validation<T> = { min, max ->
        Validation { input ->
            val size = sizeOf(input)
            errorOnFail(messageProvider(input, min, max)) { size < min || size > max }
        }
    }

    /**
     * The value of the field or property must match the regular expression defined in the regexp element.
     *
     * @param options [RegexOption] set.
     * @param messageProvider Message provider.
     * @receiver Takes input and the regex expression as args.
     * @return (String) -> Validation where regex expression is taken as arg.
     */
    fun Pattern(
        vararg options: RegexOption,
        messageProvider: (CharSequence, String) -> String = { input, expression -> "$expression is not matching $input" }
    ): (String) -> Validation<CharSequence> = { expression ->
        val regex = when {
            options.isEmpty() -> expression.toRegex()
            options.size == 1 -> expression.toRegex(options.first())
            else -> expression.toRegex(options.toSet())
        }
        Validation { input ->
            errorOnFail(messageProvider(input, expression)) {
                !regex.matches(input)
            }
        }
    }

    /**
     * The value of the field or property must be a valid email address.
     *
     * It uses [Pattern] internally.
     *
     * @param messageProvider Message provider.
     * @receiver Takes input and the regex email as args.
     * @return Validation<CharSequence>.
     */
    fun Email(messageProvider: (CharSequence, String) -> String = { input, _ -> "$input is invalid e-mail." }): Validation<CharSequence> =
        """
(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"
(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")
@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9]
(?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}
(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:
(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])
        """.trimIndent()
            .replace("\n", "")
            .let { regex ->
                ComposedValidation(
                    Validation { input ->
                        val at = input.lastIndexOf("@")
                        //error if local-part is longer than 64 characters
                        if (at > 0 && input.subSequence(0, at).length > 64) {
                            error(messageProvider(input, regex))
                        }
                    },
                    Pattern(RegexOption.IGNORE_CASE, messageProvider = messageProvider)(regex)
                )
            }

    /**
     *  Adapter for an object that has size/length props.
     *
     * @param T CharSequence, Array, Collection, Map types allowed.
     * @param value Value.
     * @return Int Size.
     */
    private fun <T> sizeOf(value: T): Int =
        when (value) {
            is CharSequence -> value.length
            is Array<*> -> value.size
            is Collection<*> -> value.size
            is Map<*, *> -> value.size
            else -> throw IllegalArgumentException(
                """
                    Unsupported type ${value!!::class.java.simpleName}, allowed: 
                    CharSequence, Array, Collection and Map
                """.trimIndent()
            )
        }

    /**
     * Number to big decimal.
     *
     * @param T Number type.
     * @return BigDecimal.
     */
    private fun <T : Number> T.toBigDecimalInternal(): BigDecimal = when (this) {
        is Float -> this.toBigDecimal()
        is Double -> this.toBigDecimal()
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Int -> this.toBigDecimal()
        is Long -> this.toBigDecimal()
        is Short -> this.toInt().toBigDecimal()
        is Byte -> this.toInt().toBigDecimal()
        else -> throw IllegalArgumentException("Unsupported type ${this::class.java.simpleName}")
    }
}
