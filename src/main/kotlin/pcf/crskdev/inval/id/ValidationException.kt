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
 * Validation exception.
 *
 * @property violations Constraint violations
 * @author Cristian Pela
 * @since 1.0
 */
class ValidationException internal constructor(val violations: List<ConstraintViolation>) : RuntimeException() {

    /**
     * Data that carries the input validation error.
     *
     * Input is represented the id assigned by user.
     *
     * @property id [Id]
     * @property message CharSequence
     */
    data class ConstraintViolation internal constructor(val id: Id, val message: CharSequence)

    companion object {
        /**
         * Creates a ValidationException with a single [ConstraintViolation] error.
         *
         * @param id [Id]
         * @param message CharSequence
         * @return ValidationException
         */
        internal fun of(id: Id, message: CharSequence): ValidationException = ValidationException(
            listOf(ConstraintViolation(id, message))
        )
    }

    /**
     * Builder for [ValidationException]
     *
     * @constructor Create empty Builder
     */
    internal class Builder {

        /**
         * Constraint violations.
         */
        private val violations = mutableListOf<ConstraintViolation>()

        /**
         * Add a new [ConstraintViolation] based on id and message.
         *
         * @param id Id
         * @param message Message.
         * @return Build
         */
        fun add(id: Id, message: CharSequence): Builder {
            this.violations.add(ConstraintViolation(id, message))
            return this
        }

        /**
         * Adds [ValidationException.violations] to the builder errors.
         *
         * @param validationException ValidationException
         * @return Build
         */
        fun add(validationException: ValidationException): Builder {
            this.violations.addAll(validationException.violations)
            return this
        }

        /**
         * Checks if there are no constrain violations.
         */
        val isEmpty: Boolean get() = this.violations.isEmpty()

        /**
         * Builds a new ValidationException
         * @return ValidationException
         */
        fun build(): ValidationException = ValidationException(violations)
    }

    override fun toString(): String {
        return this.violations.joinToString("\n")
    }
}

/**
 * Helper that builds the ValidationException and also creates a Result based on
 * the give success value.
 *
 * @param T Result success value type
 * @param success Success value
 * @return Result.
 */
internal fun <T> ValidationException.Builder.buildToResult(success: T): Result<T> = buildToResult { success }

/**
 * Helper that builds the ValidationException and also creates a Result based on
 * the give success value provider.
 *
 * @param T Result success value type
 * @param successProvider  Lambda that creates the success value.
 * @return Result
 */
internal inline fun <T> ValidationException.Builder.buildToResult(successProvider: () -> T): Result<T> =
    if (this.isEmpty) {
        Result.success(successProvider())
    } else {
        Result.failure(this.build())
    }

/**
 * Alias for creating a ValidationException based on a message.
 */
typealias ValidationExceptionProvider = (CharSequence) -> ValidationException
