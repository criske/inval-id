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
import java.util.Date
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
     * Returns _(Number) -> Validation<Number>_ lambda, where argument is the max number against which
     * input will be tested.
     *
     * Example:
     *
     * _Max(scale = 2.scale())(10.12f) validates 10.118654f withId 1_
     * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the max allowed)
     *
     * _Max()(10) validates 19 withId 1_ (fails because min is _10_ and input is _19_)
     *
     * @param messageProvider Custom message lambda. Takes the input and max as args.
     * @param scale [Scale] approximation scale applicable for floats and doubles inputs decimal scale.
     * otherwise for integers will be ignored.
     * @return (T) -> Validation<T>.
     */
    fun Max(
        scale: Scale = Scale(),
        messageProvider: (Number, Number) -> String = { input, max -> "Input $input must be at most $max." }
    ): (Number) -> Validation<Number> = { max ->
        Validation { input ->
            val predicate: (Number) -> Boolean = {
                input
                    .toBigDecimalInternal()
                    .setScale(scale.value, scale.roundingMode) > max.toBigDecimalInternal()
                    .setScale(scale.value, scale.roundingMode)
            }
            errorOnFail(messageProvider(input, max), predicate)
        }
    }

    /**
     * The value of the field or property must be a [Number] value larger than or equal
     * to the [Number] in the value element.
     * Returns _(Number) -> Validation<Number>_ lambda, where arg is the minimum number against which
     * input will be tested.
     *
     * Example:
     *
     * _Min(scale = 2.scale())(10.12f) validates 10.118654f withId 1_
     * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the min allowed)
     *
     * _Min()(10) validates 9 withId 1_ (fails because min is _10_ and input is _9_)
     *
     * @param messageProvider Custom message lambda. Takes the input and min as args.
     * @param scale [MathContext] approximation scale applicable for floats and doubles inputs decimal scale,
     * otherwise for integers will be ignored.
     * @return (Number) -> Validation<Number>.
     */
    fun Min(
        scale: Scale = Scale(),
        messageProvider: (Number, Number) -> String = { input, min -> "Input $input must be at least $min." }
    ): (Number) -> Validation<Number> = { min ->
        Validation { input ->
            val predicate: (Number) -> Boolean = {
                input
                    .toBigDecimalInternal()
                    .setScale(scale.value, scale.roundingMode) < min.toBigDecimalInternal()
                    .setScale(scale.value, scale.roundingMode)
            }
            errorOnFail(messageProvider(input, min), predicate)
        }
    }

    /**
     * Interval Rule, that uses internally [Min] and [Max] rules.
     *
     * @param messageProvider Message on fail. Lambda take Input, Min and Max values as args.
     * @param scale Decimal scale scaling. See [Min] and [Max]
     * @return Lambda that takes [Min] Number and [Max] Number as params and returns a Validation<Number>.
     */
    fun MinMax(
        scale: Scale = Scale(),
        messageProvider: (Number, Number, Number) -> String = { input, min, max -> "$input must be between [$min, $max]" }
    ): (Number, Number) -> Validation<Number> = { min, max ->
        ComposedValidation(
            Min(scale) { input, _ -> messageProvider(input, min, max) }(min),
            Max(scale) { input, _ -> messageProvider(input, min, max) }(max)
        )
    }

    /**
     * The value of the field or property must be a number within a specified range. The integer element specifies
     * the maximum integral digits for the number, and the fraction element specifies the maximum fractional
     * digits for the number.
     *
     * Example:
     *
     * _Digits()(3, 2) validates 120.32 withId 1_ passes
     *
     * _Digits()(3, 2) validates 12.32 withId 1_ fails
     *
     * See also : [DigitsInt], [DigitsStr]
     *
     * @param messageProvider Message provider on fail.
     * @receiver Takes Input, Digits and Fractions as args and returns the message.
     * @return `(Int, Int) -> Validation<Number>` that takes Digits and Fractions as args.
     */
    fun Digits(
        messageProvider: (Number, Int, Int) -> String = { input, integers, fractions -> "$input number must have $integers digits and $fractions fractions" }
    ): (Int, Int) -> Validation<Number> = { integers, fractions ->
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
        Digits { input, _, _ -> messageProvider(input.toString(), integers, fractions) }(integers, fractions)
            .adapt { BigDecimal(it) }
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
        Digits { input, _, _ -> messageProvider(input.toInt(), integers) }(integers, 0).adapt { it }
    }

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
                        // error if local-part is longer than 64 characters
                        if (at > 0 && input.subSequence(0, at).length > 64) {
                            error(messageProvider(input, regex))
                        }
                    },
                    Pattern(RegexOption.IGNORE_CASE, messageProvider = messageProvider)(regex)
                )
            }

    /**
     * The value of the field or property must be a positive number.
     *
     * @param messageProvider Message provider.
     * @receiver Takes input Number as arg.
     * @return Validation<Number>.
     */
    fun Positive(messageProvider: (Number) -> String = { input -> "$input should be positive." }): Validation<Number> =
        Validation { input ->
            errorOnFail(messageProvider(input)) { input.toBigDecimalInternal() < BigDecimal.ONE }
        }

    /**
     * The value of the field or property must be a positive or zero number.
     *
     * @param messageProvider Message provider.
     * @receiver Takes input Number as arg.
     * @return Validation<Number>.
     */
    fun PositiveOrZero(messageProvider: (Number) -> String = { input -> "$input should be positive or zero." }): Validation<Number> =
        Validation { input ->
            errorOnFail(messageProvider(input)) { input.toBigDecimalInternal() < BigDecimal.ZERO }
        }

    /**
     * The value of the field or property must be a negative number.
     *
     * @param messageProvider Message provider.
     * @receiver Takes input Number as arg.
     * @return Validation<Number>.
     */
    fun Negative(messageProvider: (Number) -> String = { input -> "$input should be negative." }): Validation<Number> =
        Validation { input ->
            errorOnFail(messageProvider(input)) { input.toBigDecimalInternal() > BigDecimal.valueOf(-1) }
        }

    /**
     * The value of the field or property must be a negative or zero number.
     *
     * @param messageProvider Message provider.
     * @receiver Takes input Number as arg.
     * @return Validation<Number>.
     */
    fun NegativeOrZero(messageProvider: (Number) -> String = { input -> "$input should be negative or zero." }): Validation<Number> =
        Validation { input ->
            errorOnFail(messageProvider(input)) { input.toBigDecimalInternal() > BigDecimal.ZERO }
        }

    /**
     * The value of the field or property must be a date in the past.
     *
     * @param nowProvider Now Date provider.
     * @receiver Provides current Date.
     * @param messageProvider Message Provider.
     * @receiver Takes input and now date reference as args.
     * @return Validation<Date>.
     */
    fun Past(
        nowProvider: () -> Date = { Date() },
        messageProvider: (Date, Date) -> String = { input, now -> "$input must be before $now" }
    ): Validation<Date> = Validation { input ->
        val now = nowProvider()
        errorOnFail(messageProvider(input, now)) { input == now || input.after(now) }
    }

    /**
     *The value of the field or property must be a date or time in the past or present.
     *
     * @param nowProvider Now Date provider.
     * @receiver Provides current Date.
     * @param messageProvider Message Provider.
     * @receiver Takes input and now date reference as args.
     * @return Validation<Date>.
     */
    fun PastOrPresent(
        nowProvider: () -> Date = { Date() },
        messageProvider: (Date, Date) -> String = { input, now -> "$input must be before or same as $now" }
    ): Validation<Date> = Validation { input ->
        val now = nowProvider()
        errorOnFail(messageProvider(input, now)) { input.after(now) }
    }

    /**
     * The value of the field or property must be a date in the future.
     *
     * @param nowProvider Now Date provider.
     * @receiver Provides current Date.
     * @param messageProvider Message Provider.
     * @receiver Takes input and now date reference as args.
     * @return Validation<Date>.
     */
    fun Future(
        nowProvider: () -> Date = { Date() },
        messageProvider: (Date, Date) -> String = { input, now -> "$input must be after $now" }
    ): Validation<Date> = Validation { input ->
        val now = nowProvider()
        errorOnFail(messageProvider(input, now)) { input == now || input.before(now) }
    }

    /**
     *The value of the field or property must be a date or time in the future or present.
     *
     * @param nowProvider Now Date provider.
     * @receiver Provides current Date.
     * @param messageProvider Message Provider.
     * @receiver Takes input and now date reference as args.
     * @return Validation<Date>.
     */
    fun FutureOrPresent(
        nowProvider: () -> Date = { Date() },
        messageProvider: (Date, Date) -> String = { input, now -> "$input must be after or same as $now" }
    ): Validation<Date> = Validation { input ->
        val now = nowProvider()
        errorOnFail(messageProvider(input, now)) { input.before(now) }
    }

    // ============================= HELPERS & UTILS =============================

    /**
     *  Adapter for an object that has size/length props.
     *
     * @param T CharSequence, Array, Collection, Map and kotlin Array Primitives types allowed.
     * @param value Value.
     * @return Int Size.
     */
    private fun <T> sizeOf(value: T): Int =
        when (value) {
            is CharSequence -> value.length
            is Array<*> -> value.size
            is CharArray -> value.size
            is ByteArray -> value.size
            is ShortArray -> value.size
            is IntArray -> value.size
            is LongArray -> value.size
            is FloatArray -> value.size
            is DoubleArray -> value.size
            is Collection<*> -> value.size
            is Map<*, *> -> value.size
            else -> throw IllegalArgumentException(
                """
                    Unsupported type ${value!!::class.java.simpleName}, allowed: 
                    CharSequence, Array, Collection, Map and Primitive Array (CharArray, IntArray etc...)
                """.trimIndent()
            )
        }

    /**
     * Number to big decimal.
     *
     * @return BigDecimal.
     */
    private fun Number.toBigDecimalInternal(): BigDecimal = when (this) {
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

    /**
     * Handy extension to create a [MathContext] used by [Min] and [Max] for decimal scaling when dealing with
     * fraction inputs.
     *
     * Receiver is the number of decimal scale after scaling will kick in.
     *
     * Example:
     *
     * "3.scale(RoundingMode.HALF_EVEN)"
     *
     * @param roundingMode [RoundingMode] strategy.
     */
    fun Int.scale(roundingMode: RoundingMode = RoundingMode.HALF_UP) = Scale(this, roundingMode)

    /**
     * Scale info needed by Min and Max rules
     *
     * @property value Value.
     * @property roundingMode [RoundingMode]
     */
    data class Scale(val value: Int = 0, val roundingMode: RoundingMode = RoundingMode.HALF_UP)
}
