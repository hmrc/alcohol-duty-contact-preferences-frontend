/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms

import base.SpecBase
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

import scala.collection.immutable.ArraySeq

class EnterEmailAddressFormProviderSpec extends StringFieldBehaviours with SpecBase {

  val requiredKey = "changePreferences.enter-email-address.empty-error"
  val invalidKey  = "changePreferences.enter-email-address.format-error"
  val emailRegex  = "^[a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$"

  val form = new EnterEmailAddressFormProvider()()

  ".emailAddress" - {

    "must bind like a mandatory field" - {
      val fieldName = "emailAddress"

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    }

    "must bind valid email addresses" in {
      val data = Map("emailAddress" -> emailAddress)

      form.bind(data).value.value mustBe emailAddress
    }

    "must bind valid email addresses without a top level domain" in {
      val data = Map("emailAddress" -> "name@example")

      form.bind(data).value.value mustBe "name@example"
    }

    "must unbind valid email addresses" in {
      val data = emailAddress
      form.fill(data).data must contain theSameElementsAs Map(
        "emailAddress" -> emailAddress
      )
    }

    "must fail to bind empty answers" in {
      val data = Map("emailAddress" -> "")

      form.bind(data).errors must contain allElementsOf Seq(
        FormError("emailAddress", requiredKey, Seq())
      )
    }

    "fail to bind invalid email addresses" - {

      "reject missing @ symbol" in {
        val data = Map("emailAddress" -> "invalidemail.com")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

      "reject missing domain" in {
        val data = Map("emailAddress" -> "test@")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

      "reject missing username" in {
        val data = Map("emailAddress" -> "@domain.com")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

      "reject spaces in email" in {
        val data = Map("emailAddress" -> "test @example.com")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

      "reject multiple @ symbols" in {
        val data = Map("emailAddress" -> "test@@example.com")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

      "reject domain with invalid characters" in {
        val data = Map("emailAddress" -> "test@exam!ple.com")
        form.bind(data).errors must contain only FormError("emailAddress", invalidKey, ArraySeq(emailRegex))
      }

    }
  }
}
