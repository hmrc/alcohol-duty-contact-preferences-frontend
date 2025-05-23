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

package utils

import base.SpecBase
import models.ErrorModel
import pages.changePreferences.ContactPreferencePage
import play.api.http.Status.BAD_REQUEST

class ExistingEmailPageCheckHelperSpec extends SpecBase {

  val testHelper = new ExistingEmailPageCheckHelper()

  "ExistingEmailPageCheckHelper .checkDetailsForExistingEmailPage" - {
    "must return a Right containing the email address if the user has an existing verified email" in {
      val result = testHelper.checkDetailsForExistingEmailPage(userAnswersPostWithEmail)

      result mustBe Right(emailAddress)
    }

    "must return a Left containing an ErrorModel if the user has no email in the subscription summary" in {
      val result = testHelper.checkDetailsForExistingEmailPage(userAnswersPostNoEmail)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on existing email page: User has no email in subscription summary.")
      )
    }

    "must return a Left containing an ErrorModel if the user's email in the subscription summary is not verified" in {
      val result = testHelper.checkDetailsForExistingEmailPage(userAnswersPostWithUnverifiedEmail)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on existing email page: User's email in subscription summary is not verified.")
      )
    }

    "must return a Left containing an ErrorModel if the user has not selected email on the contact preference page" in {
      val result = testHelper.checkDetailsForExistingEmailPage(
        userAnswersPostWithEmail.set(ContactPreferencePage, false).success.value
      )

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on existing email page: User has not selected email."))
    }
  }

}
