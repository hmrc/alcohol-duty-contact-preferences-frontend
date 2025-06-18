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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}

class PageCheckHelperSpec extends SpecBase {

  val testHelper = new PageCheckHelper()

  "checkDetailsForEnrolledEmailsPage" - {
    "must return a Right containing the email address if the user is currently on email and has selected email" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(userAnswers)

      result mustBe Right(emailAddress)
    }

    "must return a Left containing an ErrorModel if the user is currently on post" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(userAnswersPostWithEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User is currently on post."))
    }

    "must return a Left containing an ErrorModel if the user has not selected email on the contact preference page" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(
        userAnswers.set(ContactPreferencePage, false).success.value
      )

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User has not selected email."))
    }

    "must return a Left containing an ErrorModel if the user has no email in the subscription summary" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(
        userAnswers.copy(subscriptionSummary = subscriptionSummaryEmail.copy(emailAddress = None))
      )

      result mustBe Left(
        ErrorModel(
          INTERNAL_SERVER_ERROR,
          "Error on enrolled emails page: User is currently on email but has no email in subscription summary."
        )
      )
    }
  }

  "checkDetailsForExistingEmailPage" - {
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
