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
import java.nio.CharBuffer
import pcf.crskdev.inval.id.Rules.NotEmpty
import pcf.crskdev.inval.id.Rules.Pattern

internal class ValidationAdapterTest : StringSpec({

    "should adapt a input to a validation rule" {
        // adapts the CharArray input to CharSequence that could be validated by Pattern rule.
        val adaptedRule: Validation<CharArray> =
            Pattern { _, _ -> "Weak password: must at least 8 in length" }("^.{8,}$")
                .adapt { CharBuffer.wrap(it) }
        (ComposedValidation(NotEmpty(), adaptedRule) validates CharArray(0) withId 1)().isFailure shouldBe true
        (ComposedValidation(NotEmpty(), adaptedRule) validates "abcd1234".toCharArray() withId 1)().isSuccess shouldBe true
        val result = (ComposedValidation(NotEmpty(), adaptedRule) validates "abcd1".toCharArray() withId 1)()
        result.isFailure shouldBe true
        with((result.exceptionOrNull()!! as ValidationException).errors.first()) {
            message shouldBe "Weak password: must at least 8 in length"
            id shouldBe 1.toId()
        }
    }
})
