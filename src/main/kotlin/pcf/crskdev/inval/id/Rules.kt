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
     */
    object AssertTrue {

        /**
         * Invoke.
         *
         * @param message Message.
         * @return Validation<Boolean>
         */
        operator fun invoke(message: String = "Boolean flag must be true"): Validation<Boolean> = Validation { input ->
            errorOnFail(message) { !input }
        }
    }

    /**
     * The value of the field or property must be false.
     */
    object AssertFalse {

        /**
         * Invoke.
         *
         * @param message Message.
         * @return Validation<Boolean>
         */
        operator fun invoke(message: String = "Boolean flag must be false"): Validation<Boolean> = Validation { input ->
            errorOnFail(message) { input }
        }
    }

    /**
     * The value of the field or property must contain at least one non-white space character.
     *
     * @constructor Create empty Not blank
     */
    object NotBlank {

        operator fun invoke(message: String = "Field or property required"): Validation<CharSequence> =
            Validation { input -> errorOnFail(message) { input.isBlank() } }
    }

    /**
     * The value of the field or property must not be empty.
     * The length of the characters or array, and the size of a collection or map are evaluated.
     *
     * Supported types are:
     * - CharSequence (length of character sequence is evaluated)
     * - Collection (collection size is evaluated)
     * - Map (map size is evaluated)
     * - Array (array length is evaluated)
     * @constructor Create empty Not empty
     */
    object NotEmpty {

        operator fun <T> invoke(message: String = "Field or property required"): Validation<T> =
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
    }

    /**
     * The value of the field or property must be a [Number] value lower than or equal
     * to the [Number] in the value element.
     */
    object Max {

        /**
         * Invokes the rule.
         *
         * Returns _(T) -> Validation<T>_ lambda, where T param is the maximum boundary [Number] against which
         * input will be tested.
         *
         * Example:
         *
         * _Max<Float>(scale = 2 rmode RoundingMode.HALF_UP)(10.12f) validates 10.118654f withId 1_
         * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the max allowed)
         *
         * _Max<Int>()(10) validates 19 withId 1_ (fails because min is _10_ and input is _19_)
         *
         * @param T [Number] type.
         * @param message Custom message. It could have at most one %s to show the max value in it.
         * @param scale [MathContext] approximation scale applicable for floats and doubles inputs decimal places.
         * otherwise for integers will be ignored.
         * @return (T) -> Validation<T>.
         */
        operator fun <T : Number> invoke(
            message: String = "Input must be at most %s",
            scale: MathContext = MathContext.UNLIMITED
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
                errorOnFail(String.format(message, max), predicate)
            }
        }
    }

    /**
     * The value of the field or property must be a [Number] value larger than or equal
     * to the [Number] in the value element.
     */
    object Min {

        /**
         * Invokes the rule.
         *
         * Returns _(T) -> Validation<T>_ lambda, where T param is the minimum boundary [Number] against which
         * input will be tested.
         *
         * Example:
         *
         * _Min<Float>(scale = 2 rmode RoundingMode.HALF_UP)(10.12f) validates 10.118654f withId 1_
         * (succeeds because input _10.118654f_ is rounded up to _10.12f_, the min allowed)
         *
         * _Min<Int>()(10) validates 9 withId 1_ (fails because min is _10_ and input is _9_)
         *
         * @param T [Number] type.
         * @param message Custom message. It could have at most one %s to show the min value in it.
         * @param scale [MathContext] approximation scale applicable for floats and doubles inputs decimal places,
         * otherwise for integers will be ignored.
         * @return (T) -> Validation<T>.
         */
        operator fun <T : Number> invoke(
            message: String = "Input must be at least %s",
            scale: MathContext = MathContext.UNLIMITED
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
                errorOnFail(String.format(message, min), predicate)
            }
        }
    }

    /**
     * Interval Rule, that uses internally [Min] and [Max] rules.
     *
     * @param T [Number] type.
     * @param messageProvider Message on fail.
     * @param rounding Decimal places scaling. See [Min] and [Max]
     * @return Lambda that takes [Min] Number and [Max] Number as params and returns a Validation.
     */
    fun <T : Number> MinMax(
        rounding: MathContext = MathContext.UNLIMITED,
        messageProvider: (T, T) -> String = { min, max -> "Invalid interval [$min, $max]" }
    ): (T, T) -> Validation<T> = { min, max ->
        val message = messageProvider(min, max)
        ComposedValidation(Min<T>(message, rounding)(min), Max<T>(message, rounding)(max))
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
