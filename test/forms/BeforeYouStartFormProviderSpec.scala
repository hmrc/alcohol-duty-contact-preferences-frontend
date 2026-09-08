/*
 * Copyright 2026 HM Revenue & Customs
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

import models.BeforeYouStartAnswer
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class BeforeYouStartFormProviderSpec extends AnyFreeSpec with Matchers {

  val form = new BeforeYouStartFormProvider()()

  "BeforeYouStartFormProvider" - {
    "must bind post selected with no email address" in {
      form.bind(Map("contactPreferenceEmail" -> "false")).value mustBe Some(
        BeforeYouStartAnswer(contactPreferenceEmail = false, emailAddress = None)
      )
    }

    "must bind email selected with a valid email address" in {
      form.bind(Map("contactPreferenceEmail" -> "true", "emailAddress" -> "test@example.com")).value mustBe Some(
        BeforeYouStartAnswer(contactPreferenceEmail = true, emailAddress = Some("test@example.com"))
      )
    }

    "must fail to bind when nothing is selected" in {
      val boundForm = form.bind(Map.empty[String, String])
      boundForm.errors.map(_.key) must contain("contactPreferenceEmail")
    }

    "must fail to bind when email is selected but no email address is given" in {
      val boundForm = form.bind(Map("contactPreferenceEmail" -> "true"))
      boundForm.errors.map(_.key) must contain("emailAddress")
    }

    "must fail to bind when email is selected but the email address is blank" in {
      val boundForm = form.bind(Map("contactPreferenceEmail" -> "true", "emailAddress" -> "   "))
      boundForm.errors.map(_.key) must contain("emailAddress")
    }

    "must fail to bind when email is selected but the email address is badly formatted" in {
      val boundForm = form.bind(Map("contactPreferenceEmail" -> "true", "emailAddress" -> "not-an-email"))
      boundForm.errors.map(_.key) must contain("emailAddress")
    }

    "must not require an email address when post is selected, even if one is given" in {
      form.bind(Map("contactPreferenceEmail" -> "false", "emailAddress" -> "ignored@example.com")).value mustBe Some(
        BeforeYouStartAnswer(contactPreferenceEmail = false, emailAddress = None)
      )
    }
  }
}
