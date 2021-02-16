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
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import pcf.crskdev.inval.id.Rules.NotBlank
import pcf.crskdev.inval.id.Rules.NotEmpty

internal class RulesTest : DescribeSpec({

    describe("Not Empty Tests") {

        it("should apply rule") {
            (NotEmpty<String>() validates "" withId 1)().isFailure shouldBe true
            (NotEmpty<Array<Any>>() validates emptyArray() withId 1)().isFailure shouldBe true
            (NotEmpty<Collection<Any>>() validates emptyList() withId 1)().isFailure shouldBe true
            (NotEmpty<Map<Any, Any>>() validates emptyMap() withId 1)().isFailure shouldBe true
        }

        it("should throw type is not allowed") {
            shouldThrow<IllegalArgumentException> {
                (NotEmpty<Int>() validates 1 withId 1)()
            }
        }
    }

    describe("Not Blank Tests") {

        it("should apply rule") {
            (NotBlank() validates "   " withId 1)().isFailure shouldBe true
        }
    }
})
