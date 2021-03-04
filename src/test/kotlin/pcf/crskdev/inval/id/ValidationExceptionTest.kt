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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class ValidationExceptionTest : StringSpec({

    "should build exception" {
        val exception = ValidationException.Builder()
            .add(1.toId(), "error#1")
            .add(1.toId(), "error#2")
            .build()
        exception.violations shouldBe listOf(
            ValidationException.ConstraintViolation(1.toId(), "error#1"),
            ValidationException.ConstraintViolation(1.toId(), "error#2")
        )
        exception.toString() shouldBe listOf(
            ValidationException.ConstraintViolation(1.toId(), "error#1"),
            ValidationException.ConstraintViolation(1.toId(), "error#2")
        ).joinToString("\n")
    }

    "should create simple exception" {
        val exception = ValidationException.of(1.toId(), "error")
        exception.violations shouldBe listOf(
            ValidationException.ConstraintViolation(1.toId(), "error")
        )
    }

    "should build to result failure" {
        ValidationException.Builder()
            .add(1.toId(), "error#1")
            .add(1.toId(), "error#2")
            .buildToResult("foo").isFailure shouldBe true
    }

    "should build to result success" {
        ValidationException.Builder()
            .buildToResult("foo").isSuccess shouldBe true
    }

    "should build to result success with provider" {
        ValidationException.Builder()
            .buildToResult { "foo" }.isSuccess shouldBe true
    }
})
