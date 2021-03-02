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
 * Validation adapter.
 *
 * @param T current Type
 * @param R adapted Type
 * @param rule Validation Rule of Type R that input T must be adapted.
 * @param transformer Transformer.
 * @receiver Transforms T to R type.
 * @return Validation<T>
 */
@PublishedApi
internal fun <T, R> ValidationAdapter(
    rule: Validation<R>,
    transformer: (T) -> R
): Validation<T> = { input, id, _ ->
    val adaptedInput = transformer(input)
    (rule validates adaptedInput withId id)().map { input }
}

/**
 * Validation adapter extension for a Validation<R> rule.
 *
 * Used to adapt an input value type to an existing validation rule
 * that doesn't support the input value type.
 *
 * @param T current Type
 * @param R adapted Type
 * @param transformer Transformer.
 * @receiver Transforms T to R type.
 * @return Validation<T>
 */
fun <T, R> Validation<R>.adapt(transformer: (T) -> R): Validation<T> =
    ValidationAdapter(this, transformer)
