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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk

internal class InputMergeTest : StringSpec({

    val ruleStr = Validation<String> { }
    val ruleInt = Validation<Int> { }

    "should merge two inputs" {
        val inputA = ruleStr validates "" withId 1
        val inputB = ruleInt validates 1 withId 2
        val merged = inputA + inputB
        merged.shouldBeInstanceOf<InputMerge>()
        val result = merged.runValidations()
        result.getOrThrow() shouldBe listOf("", 1)
    }

    "should merge multiple inputs" {
        val inputA = ruleStr validates "a" withId 1
        val inputB = ruleInt validates 1 withId 2
        val inputC = ruleStr validates "b" withId 3
        val inputD = ruleInt validates 2 withId 4
        val inputE = ruleStr validates "c" withId 5
        val inputF = ruleInt validates 3 withId 6

        val merged = (inputA + inputB) + inputC + (inputD + inputE + inputF)
        merged().getOrThrow() shouldBe listOf("a", 1, "b", 2, "c", 3)
    }

    "should fail validation on merged inputs" {
        val inputA = Rules.NotEmpty<String>() validates "" withId 1
        val inputB = Rules.Min()(2) validates 1 withId 2
        val inputC = ruleStr validates "b" withId 3
        val merged = inputA + inputB + inputC
        val result = merged.runValidations()
        result.isFailure shouldBe true
        with((result.exceptionOrNull() as ValidationException).violations) {
            size shouldBe 2
            first().id shouldBe 1.toId()
            this[1].id shouldBe 2.toId()
        }
    }

    "should throw if input source is not supported on merge" {
        val inputA = ruleStr validates "a" withId 1
        val inputB = mockk<InputSource<*>>()
        shouldThrow<UnsupportedOperationException> {
            inputA + inputB
        }
    }
})
