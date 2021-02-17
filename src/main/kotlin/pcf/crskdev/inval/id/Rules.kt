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
            when (input) {
                is CharSequence -> errorOnFail(message) { input.isEmpty() }
                is Array<*> -> errorOnFail(message) { input.isEmpty() }
                is Collection<*> -> errorOnFail(message) { input.isEmpty() }
                is Map<*, *> -> errorOnFail(message) { input.isEmpty() }
                else -> throw IllegalArgumentException(
                    """
                        Unsupported type ${input!!::class.java.simpleName}, allowed: 
                        CharSequence, Array, Collection and Map
                    """.trimIndent()
                )
            }
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
            val predicate: (T) -> Boolean = when (input) {
                is Float -> {
                    {
                        input.toBigDecimal().setScale(scale.precision, scale.roundingMode) > max.toFloat()
                            .toBigDecimal()
                    }
                }
                is Double -> {
                    {
                        input.toBigDecimal().setScale(scale.precision, scale.roundingMode) > max.toDouble()
                            .toBigDecimal()
                    }
                }
                is BigDecimal -> {
                    {
                        input.setScale(scale.precision, scale.roundingMode) > max as BigDecimal
                    }
                }
                is BigInteger -> {
                    {
                        input > max as BigInteger
                    }
                }
                is Int -> {
                    {
                        input.toInt() > max.toInt()
                    }
                }
                is Long -> {
                    {
                        input.toLong() > max.toLong()
                    }
                }
                is Short -> {
                    {
                        input.toShort() > max.toShort()
                    }
                }
                is Byte -> {
                    {
                        input.toByte() > max.toByte()
                    }
                }
                else -> throw IllegalArgumentException("Unsupported type ${input::class.java.simpleName}")
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
            val predicate: (T) -> Boolean = when (input) {
                is Float -> {
                    {
                        input.toBigDecimal().setScale(scale.precision, scale.roundingMode) < min.toFloat()
                            .toBigDecimal()
                    }
                }
                is Double -> {
                    {
                        input.toBigDecimal().setScale(scale.precision, scale.roundingMode) < min.toDouble()
                            .toBigDecimal()
                    }
                }
                is BigDecimal -> {
                    {
                        input.setScale(scale.precision, scale.roundingMode) < min as BigDecimal
                    }
                }
                is BigInteger -> {
                    {
                        input < min as BigInteger
                    }
                }
                is Int -> {
                    {
                        input.toInt() < min.toInt()
                    }
                }
                is Long -> {
                    {
                        input.toLong() < min.toLong()
                    }
                }
                is Short -> {
                    {
                        input.toShort() < min.toShort()
                    }
                }
                is Byte -> {
                    {
                        input.toByte() < min.toByte()
                    }
                }
                else -> throw IllegalArgumentException("Unsupported type ${input::class.java.simpleName}")
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
}
