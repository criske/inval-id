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
import io.mockk.mockk
import pcf.crskdev.inval.id.Rules.Max
import pcf.crskdev.inval.id.Rules.Min
import pcf.crskdev.inval.id.Rules.NotBlank
import pcf.crskdev.inval.id.Rules.NotEmpty
import pcf.crskdev.inval.id.Rules.rmode
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

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

    describe("Min Tests") {

        it("should apply approximation to floats and doubles") {
            (Min<Float>(rounding = 2 rmode RoundingMode.HALF_UP)(10.12f) validates 10.118654f withId 1)().isSuccess shouldBe true
            (Min<Double>()(10.0) validates 9.99999999 withId 1)().isSuccess shouldBe true
        }

        it("should apply to integer numbers") {
            (Min<Int>()(10) validates 9 withId 1)().isFailure shouldBe true
            (Min<Int>()(9) validates 9 withId 1)().isSuccess shouldBe true

            (Min<Long>()(10L) validates 9L withId 1)().isFailure shouldBe true
            (Min<Long>()(9L) validates 9L withId 1)().isSuccess shouldBe true

            (Min<Short>()(10) validates 9 withId 1)().isFailure shouldBe true
            (Min<Short>()(9) validates 9 withId 1)().isSuccess shouldBe true

            (Min<Byte>()(10) validates 9 withId 1)().isFailure shouldBe true
            (Min<Byte>()(9) validates 9 withId 1)().isSuccess shouldBe true
        }

        it("should apply to BigDecimal/BigInteger") {
            (Min<BigInteger>()(BigInteger.TEN) validates BigInteger.valueOf(9) withId 1)().isFailure shouldBe true
            (Min<BigDecimal>()(BigDecimal.valueOf(1.0)) validates BigDecimal.valueOf(0.0) withId 1)().isFailure shouldBe true
        }

        it("should throw if Number type is not supported") {
            shouldThrow<IllegalArgumentException> {
                (Min<Number>()(mockk()) validates mockk() withId 1)()
            }
        }

        it("should have custom message on fail") {
            val fail = (Min<Int>("My message")(10) validates 9 withId 1)().exceptionOrNull()!! as ValidationException
            fail.errors.first().message shouldBe "My message"
        }
    }

    describe("Max Tests") {

        it("should apply approximation to floats and doubles") {
            (Max<Float>(rounding = 2 rmode RoundingMode.HALF_UP)(10.12f) validates 10.118654f withId 1)().isSuccess shouldBe true
            (Max<Double>()(10.0) validates 9.99999999 withId 1)().isSuccess shouldBe true
        }

        it("should apply to integer numbers") {
            (Max<Int>()(10) validates 19 withId 1)().isFailure shouldBe true
            (Max<Int>()(9) validates 9 withId 1)().isSuccess shouldBe true

            (Max<Long>()(10L) validates 19L withId 1)().isFailure shouldBe true
            (Max<Long>()(9L) validates 9L withId 1)().isSuccess shouldBe true

            (Max<Short>()(10) validates 19 withId 1)().isFailure shouldBe true
            (Max<Short>()(9) validates 9 withId 1)().isSuccess shouldBe true

            (Max<Byte>()(10) validates 19 withId 1)().isFailure shouldBe true
            (Max<Byte>()(9) validates 9 withId 1)().isSuccess shouldBe true
        }

        it("should apply to BigDecimal/BigInteger") {
            (Max<BigInteger>()(BigInteger.TEN) validates BigInteger.valueOf(19) withId 1)().isFailure shouldBe true
            (Max<BigDecimal>()(BigDecimal.valueOf(0.0)) validates BigDecimal.valueOf(1.0) withId 1)().isFailure shouldBe true
        }

        it("should throw if Number type is not supported") {
            shouldThrow<IllegalArgumentException> {
                (Max<Number>()(mockk()) validates mockk() withId 1)()
            }
        }

        it("should have custom message on fail") {
            val fail = (Max<Int>("My message")(10) validates 19 withId 1)().exceptionOrNull()!! as ValidationException
            fail.errors.first().message shouldBe "My message"
        }
    }
})
