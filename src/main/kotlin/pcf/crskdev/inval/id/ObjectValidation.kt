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

/**
 * Entry point for Object validation.
 *
 * @param T Object type.
 * @param block For object property validations.
 * @receiver Applies ObjectValidationScope.
 * @return Validation.
 */
fun <T> ObjectValidation(block: ObjectValidationScope<T>.() -> Unit): Validation<T> = { input, _, _ ->
    val inputs = ObjectValidationScope(input).apply(block).inputs
    val builder = ValidationException.Builder()
    inputs.map { it() }.forEach {
        it.onFailure { t ->
            builder.add(t as ValidationException)
        }
    }
    builder.buildToResult(input)
}

/**
 * Object validation scope.
 *
 * @param T Input type.
 * @property input Input value.
 * @constructor Create empty Object validation scope.
 */
class ObjectValidationScope<T> internal constructor(val input: T) {

    /**
     * Property Inputs.
     */
    internal val inputs = mutableListOf<Input<*>>()

    /**
     * Extension for the Function that will apply the [Id] to [Input].
     * Part of declarative validation chain. See [validates].
     *
     * @param T Input type.
     * @param id Id.
     * @return Input.
     */
    infix fun <T> ((Id) -> Input<T>).withId(id: Id) {
        inputs.add(this(id))
    }

    /**
     * Extension for the Function that will apply the [Id].
     * Part of declarative validation chain. See [validates].
     *
     * @param T Input type.
     * @param id Any value that will be converted to [Id].
     * @return Input.
     */
    infix fun <T> ((Id) -> Input<T>).withId(id: Any) {
        inputs.add(this(id.toId()))
    }
}
