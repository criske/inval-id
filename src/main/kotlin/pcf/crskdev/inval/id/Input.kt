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

package pcf.crskdev.inval.id

/**
 * Applies [Validation] rules to a input field value associated with an [Id].
 *
 * Failing validation a will result in a [ValidationException] wrapped into a [Result.failure].
 *
 * Validations rules will be checked in order and will return a [Result.failure] on the first rule fail.
 *
 * ```
 * val emailRule = Validation<String> {
 *      if (!it.matches("^(.+)@(.+)$".toRegex())) {
 *          error("\"$it\" is not a valid email address")
 *      }
 *  }
 * Input("#email".toId(), "bad-email", emailRule)()
 *  .onFailure {
 *    // will get a ValidationException with a Field entry of #email id
 *    // and message: "bad-email" is not a valid email address"
 *  }
 * ```
 * @param T field input value type.
 * @property id Field associated [Id]
 * @property input Value.
 * @property validations Validation rules.
 * @constructor Create empty Input
 * @author Cristian Pela
 * @since 1.0
 */
@Suppress("MemberVisibilityCanBePrivate")
class Input<T>(
    val id: Id,
    val input: T,
    internal vararg val validations: Validation<T>
) {

    /**
     * Applies validation rules to the input value.
     *
     * @return Result.
     */
    operator fun invoke(): Result<T> = ComposedValidation(*this.validations)(this.input, this.id) {
        ValidationException.of(this.id, it)
    }

    companion object {

        /**
         * Input value is skipped from validation.
         * Field id associated to this will be [Id.None]
         *
         * @param T Input Type.
         * @param input Value.
         * @return Result.success
         */
        fun <T> byPass(input: T): Input<T> = Input(Id.None.Instance, input)

        /**
         * Merges multiple [Input]s and validates all into one Result.
         *
         * Using this method is discouraged since is not type safe. One of the
         * [Input.merge] overloads should be used when possible.
         *
         * @param inputs Inputs.
         * @return Result.
         */
        fun mergeAny(vararg inputs: Input<*>): Result<List<Any>> {
            val results: MutableList<Any> = mutableListOf()
            val builder = ValidationException.Builder()
            for (i in 0..inputs.lastIndex) {
                inputs[i]()
                    .onSuccess { results.add(it!!) }
                    .onFailure { builder.add(it as ValidationException) }
            }
            return builder.buildToResult { results }
        }

        /**
         * Merges two [Input]s and validates all into one Result.
         *
         * @param T
         * @param R
         * @param inputOne
         * @param inputTwo
         * @return Result
         */
        fun <T, R> merge(inputOne: Input<T>, inputTwo: Input<R>): Result<Pair<T, R>> {
            val builder = ValidationException.Builder()

            val resultOne = inputOne().onFailure {
                builder.add(it as ValidationException)
            }
            val resultTwo = inputTwo().onFailure {
                builder.add(it as ValidationException)
            }

            return builder.buildToResult {
                Pair(resultOne.getOrNull()!!, resultTwo.getOrNull()!!)
            }
        }

        /**
         * Merges three [Input]s and validates all into one Result.
         *
         * @param T
         * @param R
         * @param P
         * @param inputOne
         * @param inputTwo
         * @param inputThree
         * @return Result.
         */
        fun <T, R, P> merge(inputOne: Input<T>, inputTwo: Input<R>, inputThree: Input<P>): Result<Triple<T, R, P>> {
            val builder = ValidationException.Builder()

            val resultMergeTwo = merge(inputOne, inputTwo).onFailure {
                builder.add(it as ValidationException)
            }
            val resultThree = inputThree().onFailure {
                builder.add(it as ValidationException)
            }
            return builder.buildToResult {
                val mergeTwo = resultMergeTwo.getOrNull()!!
                Triple(mergeTwo.first, mergeTwo.second, resultThree.getOrNull()!!)
            }
        }
    }
}
