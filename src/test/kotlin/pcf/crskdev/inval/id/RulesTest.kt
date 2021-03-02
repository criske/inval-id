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
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Calendar
import java.util.Date
import pcf.crskdev.inval.id.Rules.AssertFalse
import pcf.crskdev.inval.id.Rules.AssertTrue
import pcf.crskdev.inval.id.Rules.Digits
import pcf.crskdev.inval.id.Rules.DigitsInt
import pcf.crskdev.inval.id.Rules.DigitsStr
import pcf.crskdev.inval.id.Rules.Email
import pcf.crskdev.inval.id.Rules.Future
import pcf.crskdev.inval.id.Rules.FutureOrPresent
import pcf.crskdev.inval.id.Rules.Max
import pcf.crskdev.inval.id.Rules.Min
import pcf.crskdev.inval.id.Rules.MinMax
import pcf.crskdev.inval.id.Rules.Negative
import pcf.crskdev.inval.id.Rules.NegativeOrZero
import pcf.crskdev.inval.id.Rules.NotBlank
import pcf.crskdev.inval.id.Rules.NotEmpty
import pcf.crskdev.inval.id.Rules.Past
import pcf.crskdev.inval.id.Rules.PastOrPresent
import pcf.crskdev.inval.id.Rules.Positive
import pcf.crskdev.inval.id.Rules.PositiveOrZero
import pcf.crskdev.inval.id.Rules.Size
import pcf.crskdev.inval.id.Rules.places

internal class RulesTest : DescribeSpec({

    describe("Asserts test") {
        it("should apply assert true") {
            (AssertTrue() validates false withId 1)().isFailure shouldBe true
            (AssertTrue() validates true withId 1)().isSuccess shouldBe true
        }

        it("should apply assert false") {
            (AssertFalse() validates true withId 1)().isFailure shouldBe true
            (AssertFalse() validates false withId 1)().isSuccess shouldBe true
        }
    }

    describe("Not Empty Tests") {

        it("should apply rule") {
            (NotEmpty<String>() validates "" withId 1)().isFailure shouldBe true
            (NotEmpty<Array<Any>>() validates emptyArray() withId 1)().isFailure shouldBe true
            (NotEmpty<Collection<Any>>() validates emptyList() withId 1)().isFailure shouldBe true
            (NotEmpty<Map<Any, Any>>() validates emptyMap() withId 1)().isFailure shouldBe true
            (NotEmpty<CharArray>() validates CharArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<IntArray>() validates IntArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<ByteArray>() validates ByteArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<ShortArray>() validates ShortArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<LongArray>() validates LongArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<FloatArray>() validates FloatArray(0) withId 1)().isFailure shouldBe true
            (NotEmpty<DoubleArray>() validates DoubleArray(0) withId 1)().isFailure shouldBe true
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
            (Min<Float>(scale = 2.places())(10.12f) validates 10.118654f withId 1)().isSuccess shouldBe true
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
            val fail = (Min<Int> { input, min -> "Input $input must be min $min" }(10) validates 9 withId 1)()
                .exceptionOrNull()!! as ValidationException
            fail.errors.first().message shouldBe "Input 9 must be min 10"
        }
    }

    describe("Max Tests") {

        it("should apply approximation to floats and doubles") {
            (Max<Float>(scale = 2.places())(10.12f) validates 10.118654f withId 1)().isSuccess shouldBe true
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
            val fail = (Max<Int> { input, max -> "Input $input must be max $max" }(10) validates 19 withId 1)()
                .exceptionOrNull()!! as ValidationException
            fail.errors.first().message shouldBe "Input 19 must be max 10"
        }
    }

    describe("Min Max tests") {
        it("should apply min max") {
            val minMax = MinMax<Int>()(10, 20)
            (minMax validates 15 withId 1)().isSuccess shouldBe true
            (minMax validates 10 withId 1)().isSuccess shouldBe true
            (minMax validates 20 withId 1)().isSuccess shouldBe true

            (minMax validates 9 withId 1)().isFailure shouldBe true
            (minMax validates 29 withId 1)().isFailure shouldBe true
        }

        it("should have custom message") {
            val minMax = MinMax<Int> { input, min, max -> "Bad input value $input. Must be between $min and $max" }
            val fail = (minMax(10, 20) validates 25 withId 1)().exceptionOrNull()!! as ValidationException
            fail.errors.first().message shouldBe "Bad input value 25. Must be between 10 and 20"
        }
    }

    describe("Digits tests") {
        it("should apply to int") {
            (Digits<Int>()(3, 0) validates 10 withId 1)().isFailure shouldBe true
            (Digits<Int>()(3, 0) validates 100 withId 1)().isSuccess shouldBe true
            (DigitsInt()(3) validates 120 withId 1)().isSuccess shouldBe true
            (DigitsInt()(3) validates 12 withId 1)().isFailure shouldBe true
        }
        it("should apply to floats/doubles") {
            (Digits<Double>()(3, 2) validates 103.22 withId 1)().isSuccess shouldBe true
            (Digits<Float>()(3, 2) validates 10.202f withId 1)().isFailure shouldBe true
            (Digits<Float>()(3, 2) validates 100.22f withId 1)().isSuccess shouldBe true
            (Digits<Float>()(1, 5) validates 1.12345f withId 1)().isSuccess shouldBe true
        }
        it("should apply to string numbers") {
            (DigitsStr()(3, 2) validates "120.20" withId 1)().isSuccess shouldBe true
        }
    }

    describe("Size tests") {
        it("should apply to string") {
            (Size<String>()(5, 10) validates "12345" withId 1)().isSuccess shouldBe true
            (Size<String>()(5, 10) validates "1234" withId 1)().isFailure shouldBe true
            (Size<String>()(5, 10) validates "12345678910" withId 1)().isFailure shouldBe true
        }
        it("should apply to array") {
            (Size<Array<*>>()(1, 3) validates arrayOf(1, 2, 3) withId 1)().isSuccess shouldBe true
            (Size<Array<*>>()(1, 3) validates emptyArray<Int>() withId 1)().isFailure shouldBe true
            (Size<Array<*>>()(1, 3) validates arrayOf(1, 2, 3, 4) withId 1)().isFailure shouldBe true
        }
        it("should apply to collection") {
            (Size<List<*>>()(1, 3) validates listOf(1, 2, 3) withId 1)().isSuccess shouldBe true
            (Size<List<*>>()(1, 3) validates emptyList<Int>() withId 1)().isFailure shouldBe true
            (Size<List<*>>()(1, 3) validates listOf(1, 2, 3, 4) withId 1)().isFailure shouldBe true
        }
        it("should apply to map") {
            (Size<Map<*, *>>()(1, 3) validates mapOf(1 to 1, 2 to 2, 3 to 3) withId 1)().isSuccess shouldBe true
            (Size<Map<*, *>>()(1, 3) validates emptyMap<Int, Int>() withId 1)().isFailure shouldBe true
            (Size<Map<*, *>>()(1, 3) validates mapOf(1 to 1, 2 to 2, 3 to 3, 4 to 4) withId 1)().isFailure shouldBe true
        }
        it("should throw when type not allowed") {
            shouldThrow<IllegalArgumentException> {
                (Size<Any>()(1, 3) validates Any() withId 1)()
            }
        }
    }

    describe("Pattern test") {
        it("should apply to regex pattern") {
            (Rules.Pattern()("\\d+") validates "12435" withId 1)().isSuccess shouldBe true
            (Rules.Pattern(RegexOption.IGNORE_CASE)("\\d+") validates "12435" withId 1)().isSuccess shouldBe true
            (Rules.Pattern(RegexOption.IGNORE_CASE, RegexOption.COMMENTS)("\\d+") validates "12435" withId 1)().isSuccess shouldBe true
            (Rules.Pattern()("\\d+") validates "12435f" withId 1)().isFailure shouldBe true
        }
    }

    describe("Email test") {

        val email = Email()

        it("should apply email regex and be valid") {
            val check: (CharSequence) -> Unit = {
                (email validates it withId 1)().isSuccess shouldBe true
            }
            check("email@example.com")
            check("firstname.lastname@example.com")
            check("email@subdomain.example.com")
            check("firstname+lastname@example.com")
            check("email@123.123.123.123")
            check("email@[123.123.123.123]")
            check("\"email\"@example.com")
            check("1234567890@example.com")
            check("email@example-one.com")
            check("_______@example.com")
            check("email@example.name")
            check("email@example.museum")
            check("email@example.co.jp")
            check("firstname-lastname@example.com")
            check("mailhost!username@example.org")
            check("\"john..doe\"@example.org")
            check("user%example.com@example.org")
        }

        it("should apply email regex and be invalid") {
            val check: (CharSequence) -> Unit = {
                (email validates it withId 1)().isFailure shouldBe true
            }
            check("#@%^%#\$@#\$@#.com")
            check("@example.com")
            check("Joe Smith <email@example.com>")
            check("email.example.com")
            check("email@example@example.com")
            check(".email@example.com")
            check("email.@example.com")
            check("email..email@example.com")
            check("あいうえお@example.com")
            check("email@example.com (Joe Smith)")
            check("email@example")
            check("email@-example.com")
            check("email@example..com")
            check("Abc..123@example.com")
            check("\"(),:;<>[\\]@example.com")
            check("just”not”right@example.com")
            check("this\\ is\"really\"not\\allowed@example.com")
            check("i_like_underscore@but_its_not_allowed_in_this_part.example.com")
            check("this is\"not\\allowed@example.com")
            check("a\"b(c)d,e:f;g<h>i[j\\k]l@example.com")
            check("this is\"not\\allowed@example.com")
            check("this\\ still\\\"not\\\\allowed@example.co")
            check("just\"not\"right@example.com")
            check("A@b@c@example.com")
            check("1234567890123456789012345678901234567890123456789012345678901234+x@example.com")
        }
    }

    describe("Positive tests") {
        it("should apply positive to number") {
            (Positive() validates 1 withId 1)().isSuccess shouldBe true
            (Positive() validates 1.0 withId 1)().isSuccess shouldBe true
            (Positive() validates 1.0f withId 1)().isSuccess shouldBe true
            (Positive() validates BigDecimal.ONE withId 1)().isSuccess shouldBe true
            (Positive() validates 0 withId 1)().isFailure shouldBe true
        }
        it("should apply positive or zero to number") {
            (PositiveOrZero() validates 0 withId 1)().isSuccess shouldBe true
            (PositiveOrZero() validates 0.0 withId 1)().isSuccess shouldBe true
            (PositiveOrZero() validates 0.0f withId 1)().isSuccess shouldBe true
            (PositiveOrZero() validates BigDecimal.ZERO withId 1)().isSuccess shouldBe true
            (PositiveOrZero() validates -1 withId 1)().isFailure shouldBe true
        }
    }

    describe("Negative tests") {
        it("should apply negative to number") {
            (Negative() validates -1 withId 1)().isSuccess shouldBe true
            (Negative() validates -1.0 withId 1)().isSuccess shouldBe true
            (Negative() validates -1.0f withId 1)().isSuccess shouldBe true
            (Negative() validates BigDecimal.valueOf(-1) withId 1)().isSuccess shouldBe true
            (Negative() validates 0 withId 1)().isFailure shouldBe true
        }
        it("should apply negative or zero to number") {
            (NegativeOrZero() validates 0 withId 1)().isSuccess shouldBe true
            (NegativeOrZero() validates 0.0 withId 1)().isSuccess shouldBe true
            (NegativeOrZero() validates 0.0f withId 1)().isSuccess shouldBe true
            (NegativeOrZero() validates BigDecimal.ZERO withId 1)().isSuccess shouldBe true
            (NegativeOrZero() validates 1 withId 1)().isFailure shouldBe true
        }
    }

    describe("Dates tests") {
        val now = Date()
        fun Date.add(field: Int, amount: Int): Date =
            Calendar.getInstance().run {
                add(field, amount)
                return time
            }

        it("should apply past rule date") {
            (Past({ now }) validates now.add(Calendar.DAY_OF_MONTH, -2) withId 1)().isSuccess shouldBe true
            (Past({ now }) validates now.add(Calendar.DAY_OF_MONTH, 2) withId 1)().isFailure shouldBe true
            (Past({ now }) validates now withId 1)().isFailure shouldBe true
        }

        it("should apply past or present rule date") {
            (PastOrPresent({ now }) validates now.add(Calendar.DAY_OF_MONTH, -2) withId 1)().isSuccess shouldBe true
            (PastOrPresent({ now }) validates now.add(Calendar.DAY_OF_MONTH, 2) withId 1)().isFailure shouldBe true
            (PastOrPresent({ now }) validates now withId 1)().isSuccess shouldBe true
        }

        it("should apply future rule date") {
            (Future({ now }) validates now.add(Calendar.DAY_OF_MONTH, 2) withId 1)().isSuccess shouldBe true
            (Future({ now }) validates now.add(Calendar.DAY_OF_MONTH, -2) withId 1)().isFailure shouldBe true
            (Future({ now }) validates now withId 1)().isFailure shouldBe true
        }

        it("should apply future or present rule date") {
            (FutureOrPresent({ now }) validates now.add(Calendar.DAY_OF_MONTH, 2) withId 1)().isSuccess shouldBe true
            (FutureOrPresent({ now }) validates now.add(Calendar.DAY_OF_MONTH, -2) withId 1)().isFailure shouldBe true
            (FutureOrPresent({ now }) validates now withId 1)().isSuccess shouldBe true
        }
    }
})
