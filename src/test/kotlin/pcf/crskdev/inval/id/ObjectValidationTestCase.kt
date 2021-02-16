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

class ObjectValidationTestCase : StringSpec({

    "should validate object" {
        val account = Account(
            "xx", "hunter2",
            Info("", "", "")
        )
        val empty = Validation<CharSequence> {
            errorOnFail("Field Required") { it.isBlank() }
        }
        val emailRule = RegexValidation("^(.+)@(.+)$")("Not a valid email.")

        val input = ObjectValidation<Account> {
            ComposedValidation(empty, emailRule) validates input.email withId "email"
            ObjectValidation<Info> {
                empty validates input.address withId "address"
                empty validates input.phone withId "phone".toId()
            } validates input.info withId "info"
        } validates account withId "account"

        input.validations.size shouldBe 1

        val failure = shouldThrow<ValidationException> {
            input().getOrThrow()
        }

        failure.errors shouldBe listOf(
            ValidationException.Field("email".toId(), "Not a valid email."),
            ValidationException.Field("address".toId(), "Field Required"),
            ValidationException.Field("phone".toId(), "Field Required")
        )
    }
})

data class Account(val email: String, val password: String, val info: Info)

data class Info(val phone: String, val about: String, val address: String)
