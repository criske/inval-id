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
 * Built-in validation rules offered by inval-id that
 * loosely follow [javax.validation.constraints](https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/package-frame.html)
 *
 * @constructor Create empty Rules
 */
object Rules {

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
}
