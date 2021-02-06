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

package pcf.crskdev.inval.id/*
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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

internal class InputTestCase : StringSpec({

    "should by pass rule" {
        val byPass = Input.byPass("foo")
        byPass.id.toString() shouldBe "#<no-id>"
        byPass.id.shouldBeInstanceOf<Id.None>()
        byPass.input shouldBe "foo"
        byPass().isSuccess shouldBe true
    }

    "should pass rule" {
        val rule = Validation<String> { }
        val input = Input("id".toId(), "foo", rule)
        input.id.toString() shouldBe "id"
        input.id.shouldBeInstanceOf<Id.Of>()
        (input.id as Id.Of).value shouldBe "id"
        input().isSuccess shouldBe true
    }

    "should fail rule" {
        val rule = Validation<String> { this.error("$it failed") }
        with(Input("id".toId(), "foo", rule)()) {
            this.isFailure shouldBe true
            (this.exceptionOrNull() as ValidationException)
                .errors[0] shouldBe ValidationException.Field("id".toId(), "foo failed")
        }
    }

    "should accept only ValidationException result failure construction" {
        val rule: Validation<String> = { _, _ ->
            Result.failure(RuntimeException("Some exception"))
        }
        shouldThrow<IllegalStateException> {
            Input("id".toId(), "foo", rule)()
        }
    }

    "should merge any" {
        val rule = Validation<Int> { }
        Input.mergeAny(
            Input(1.toId(), 1, rule),
            Input(2.toId(), 2, rule),
            Input(3.toId(), 3, rule),
            Input(4.toId(), 4, rule),
        ).isSuccess shouldBe true
    }

    "should merge any inputs but some should be invalid" {
        val rule = Validation<Int> { }
        val otherRule = Validation<Int> { this.error("") }
        Input.mergeAny(
            Input(1.toId(), 1, rule),
            Input(2.toId(), 2, rule),
            Input(3.toId(), 3, rule, otherRule),
            Input(4.toId(), 4, otherRule),
        ).isFailure shouldBe true
    }

    "should merge two inputs" {
        val rule = Validation<Int> { }
        Input.merge(
            Input(1.toId(), 1, rule),
            Input(2.toId(), 2, rule)
        ).isSuccess shouldBe true
    }

    "should merge two inputs but one should be invalid" {
        val rule = Validation<Int> { }
        val otherRule = Validation<Int> { this.error("") }
        Input.merge(
            Input(1.toId(), 1, rule, otherRule),
            Input(2.toId(), 2, rule, otherRule)
        ).isFailure shouldBe true
    }

    "should merge three inputs" {
        val rule = Validation<Int> { }
        Input.merge(
            Input(1.toId(), 1, rule),
            Input(2.toId(), 2, rule),
            Input(3.toId(), 3, rule)
        ).isSuccess shouldBe true
    }

    "should merge three inputs but some should be invalid" {
        val rule = Validation<Int> { }
        val otherRule = Validation<Int> { this.error("") }
        val merge = Input.merge(
            Input(1.toId(), 1, rule),
            Input(2.toId(), 2, rule, otherRule),
            Input(3.toId(), 3, rule, otherRule)
        )

        with(merge) {
            this.isFailure shouldBe true
            (this.exceptionOrNull() as ValidationException).errors.size shouldBe 2
        }
    }

    "should use regex validation" {
        var email = RegexValidation("^(.+)@(.+)$")("Not a valid email.")
        Input(0.toId(), "foo", email)().isFailure shouldBe true
        Input(0.toId(), "foo@email.com", email)().isSuccess shouldBe true

        email = RegexValidation("^(.+)@(.+)$", setOf(RegexOption.MULTILINE))("Not a valid email.")
        Input(0.toId(), "foo", email)().isFailure shouldBe true
        Input(0.toId(), "foo@email.com", email)().isSuccess shouldBe true

        email = RegexValidation("^(.+)@(.+)$", setOf(RegexOption.MULTILINE, RegexOption.COMMENTS))("Not a valid email.")
        Input(0.toId(), "foo", email)().isFailure shouldBe true
        Input(0.toId(), "foo@email.com", email)().isSuccess shouldBe true
    }

    "should use custom message" {
        val customMessage: CustomMessageValidation<String> = { msg ->
            Validation {
                error(msg)
            }
        }
        val exception = Input(0.toId(), "foo", customMessage("Custom invalid message"))()
            .exceptionOrNull()!! as ValidationException
        exception
            .errors
            .first()
            .message shouldBe "Custom invalid message"
    }

    "should use errorOnFail from ValidationScope" {
        val rule = Validation<String> {
            errorOnFail("Is empty") { input -> input.isEmpty() }
        }
        val exception = Input(0.toId(), "", rule)()
            .exceptionOrNull()!! as ValidationException
        exception
            .errors
            .first()
            .message shouldBe "Is empty"
    }

    "should not use errorOnFail from ValidationScope" {
        val rule = Validation<String> {
            errorOnFail("Is empty") { input -> input.isEmpty() }
        }
        Input(0.toId(), "foo", rule)().isSuccess shouldBe true
    }

    "should use validation declarative form" {
        val ruleA = Validation<String> {}
        val ruleB = Validation<String> {}

        var input = ruleA validates "foo" withId 1.toId()

        input.id shouldBe Id.Of(1)
        input.input shouldBe "foo"

        input = ruleA validates "foo" withId 1

        input.id shouldBe Id.Of(1)
        input.input shouldBe "foo"

        input = composed(ruleA, ruleB) validates "foo" withId 1

        input.id shouldBe Id.Of(1)
        input.input shouldBe "foo"
    }
})
