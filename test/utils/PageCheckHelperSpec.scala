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

  // TODO: Purpose and position of this page will change under Elmer's suggestions
  "checkDetailsForEnrolledEmailsPage" - {
    "must return a Right containing the email address if the user is currently on email and has selected email" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(
        userAnswers.set(ContactPreferencePage, true).success.value
      )

      result mustBe Right(emailAddress)
    }

    "must return a Left containing an ErrorModel if the user is currently on post" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(userAnswersPostWithEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User is currently on post."))
    }

    "must return a Left containing an ErrorModel if the user has not selected email on the contact preference page" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(userAnswers)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User has not selected email."))
    }

    "must return a Left containing an ErrorModel if the user has no email in the subscription summary" in {
      val result = testHelper.checkDetailsForEnrolledEmailsPage(
        userAnswers
          .copy(subscriptionSummary = subscriptionSummaryEmail.copy(emailAddress = None))
          .set(ContactPreferencePage, true)
          .success
          .value
      )

      result mustBe Left(
        ErrorModel(
          INTERNAL_SERVER_ERROR,
          "Error on enrolled emails page: User is currently on email but has no email in subscription summary."
        )
      )
    }
  }

  // TODO: This page will be removed under Elmer's suggestions
  "checkDetailsForEnrolledLettersPage" - {
    "must return a Right containing Unit if the user is currently on post and has selected post" in {
      val result = testHelper.checkDetailsForEnrolledLettersPage(
        userAnswersPostNoEmail.set(ContactPreferencePage, false).success.value
      )

      result mustBe Right(())
    }

    "must return a Left containing an ErrorModel if the user is currently on email" in {
      val result = testHelper.checkDetailsForEnrolledLettersPage(userAnswers)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled letters page: User is currently on email."))
    }

    "must return a Left containing an ErrorModel if the user has not selected post on the contact preference page" in {
      val result = testHelper.checkDetailsForEnrolledLettersPage(userAnswersPostNoEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on enrolled letters page: User has not selected post."))
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

    "must return a Left containing an ErrorModel if the contact preference in user answers is not email" in {
      val result = testHelper.checkDetailsForExistingEmailPage(
        userAnswersPostWithEmail.set(ContactPreferencePage, false).success.value
      )

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on existing email page: Contact preference in user answers is not email.")
      )
    }
  }

  "checkDetailsForCorrespondenceAddressPage" - {
    "must return a Right containing Unit if the user is currently on email and contact preference in user answers is post" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(userAnswers)

      result mustBe Right(())
    }

    "must return a Left containing an ErrorModel if the user is currently on post" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(userAnswersPostNoEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on correspondence address page: User is currently on post."))
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not post" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(
        userAnswers.set(ContactPreferencePage, true).success.value
      )

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on correspondence address page: Contact preference in user answers is not post.")
      )
    }
  }

  "checkDetailsForCheckYourAnswers" - {
    "must return a Right containing false if the contact preference in user answers is post" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswers)

      result mustBe Right(false)
    }

    "must return a Right containing false if the contact preference is email and the user's email is already in the set of verified email addresses" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostWithEmail)

      result mustBe Right(false)
    }

    "must return a Right containing true if the contact preference is email and the email is not in the set of verified email addresses" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostNoEmail)

      result mustBe Right(true)
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not set" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(emptyUserAnswers)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on Check Your Answers: User answers do not contain the required data.")
      )
    }

    "must return a Left containing an ErrorModel if the contact preference is email but user has not provided an email address" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostNoEmail.copy(emailAddress = None))

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on Check Your Answers: User answers do not contain the required data.")
      )
    }
  }

  "checkDetailsToCreateSubmission" - {
    "must return a Right containing the correct submission details if the contact preference in user answers is post" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswers)

      result mustBe Right(contactPreferenceSubmissionPost)
    }

    "must return a Right containing the correct submission details if the contact preference is email and the email is verified" in {
      val result =
        testHelper.checkDetailsToCreateSubmission(userAnswersPostWithEmail)

      result mustBe Right(contactPreferenceSubmissionEmail)
    }

    "must return a Left containing an ErrorModel if the contact preference is email and the email is not verified" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswersPostNoEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error creating submission: Email address is not verified."))
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not set" in {
      val result = testHelper.checkDetailsToCreateSubmission(emptyUserAnswers)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error creating submission: User answers do not contain the required data.")
      )
    }

    "must return a Left containing an ErrorModel if the contact preference is email but user has not provided an email address" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswersPostNoEmail.copy(emailAddress = None))

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error creating submission: User answers do not contain the required data.")
      )
    }
  }
}
