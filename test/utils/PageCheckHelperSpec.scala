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
import models.{ErrorModel, PaperlessPreferenceSubmission}
import pages.changePreferences.ContactPreferencePage
import play.api.http.Status.{BAD_REQUEST, CONFLICT, INTERNAL_SERVER_ERROR}

class PageCheckHelperSpec extends SpecBase {

  val testHelper = new PageCheckHelper()

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

    "must return a Left containing an ErrorModel if the user has a bounced email" in {
      val result = testHelper.checkDetailsForExistingEmailPage(userAnswersPostWithBouncedEmail)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on existing email page: User has a bounced email.")
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

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on existing email page: User has not selected email.")
      )
    }
  }

  "checkDetailsForCorrespondenceAddressPage" - {
    "must return a Right containing Unit if the user is currently on email and has selected post" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(userAnswers)

      result mustBe Right(())
    }

    "must return a Left containing an ErrorModel if the user is currently on post" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(userAnswersPostNoEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error on correspondence address page: User is currently on post."))
    }

    "must return a Left containing an ErrorModel if the user has not selected post on the contact preference page" in {
      val result = testHelper.checkDetailsForCorrespondenceAddressPage(
        userAnswers.set(ContactPreferencePage, true).success.value
      )

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on correspondence address page: User has not selected post.")
      )
    }
  }

  "checkDetailsForCheckYourAnswers" - {
    "must return a Right containing false if the user has selected post" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswers)

      result mustBe Right(false)
    }

    "must return a Right containing false if the user has selected email and the email is already in the set of verified email addresses" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostWithEmail)

      result mustBe Right(false)
    }

    "must return a Right containing true if the user has selected email and the email is not in the set of verified email addresses" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostNoEmail)

      result mustBe Right(true)
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not set" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(emptyUserAnswers)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on Check Your Answers: User answers do not contain the required data.")
      )
    }

    "must return a Left containing an ErrorModel if the user has selected email but not provided an email address" in {
      val result = testHelper.checkDetailsForCheckYourAnswers(userAnswersPostNoEmail.copy(emailAddress = None))

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error on Check Your Answers: User answers do not contain the required data.")
      )
    }
  }

  "checkDetailsToCreateSubmission" - {
    "must return a Right containing the correct submission details if the user has selected post" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswers)

      result mustBe Right(contactPreferenceSubmissionPost)
    }

    "must return a Right containing the correct submission details if the user has selected email and the email is verified" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswersPostWithEmail)

      result mustBe Right(contactPreferenceSubmissionEmail)
    }

    "must return a Left containing an ErrorModel if the user has selected email and the email is not verified" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswersPostNoEmail)

      result mustBe Left(ErrorModel(BAD_REQUEST, "Error creating submission: Email address is not verified."))
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not set" in {
      val result = testHelper.checkDetailsToCreateSubmission(emptyUserAnswers)

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error creating submission: User answers do not contain the required data.")
      )
    }

    "must return a Left containing an ErrorModel if the user has selected email but not provided an email address" in {
      val result = testHelper.checkDetailsToCreateSubmission(userAnswersPostNoEmail.copy(emailAddress = None))

      result mustBe Left(
        ErrorModel(BAD_REQUEST, "Error creating submission: User answers do not contain the required data.")
      )
    }

    "must return a Left with CONFLICT when user submits the same email as their existing email" in {
      val subscriptionEmail        = "existing@example.com"
      val userAnswersWithSameEmail = userAnswersPostWithEmail.copy(
        subscriptionSummary = userAnswersPostWithEmail.subscriptionSummary.copy(
          emailAddress = Some(subscriptionEmail),
          paperlessReference = true
        ),
        emailAddress = Some(subscriptionEmail)
      )

      val result = testHelper.checkDetailsToCreateSubmission(userAnswersWithSameEmail)

      result mustBe Left(ErrorModel(CONFLICT, "Email matches existing subscription"))
    }

    "must return Right with submission when user submits different email from subscribed email" in {
      val subscriptionEmail = "existing@example.com"
      val newEmail          = "new@example.com"

      val userAnswersWithNewEmail = userAnswersPostWithEmail.copy(
        subscriptionSummary = userAnswersPostWithEmail.subscriptionSummary.copy(
          emailAddress = Some(subscriptionEmail),
          paperlessReference = true
        ),
        emailAddress = Some(newEmail),
        verifiedEmailAddresses = Set(newEmail)
      )

      val result = testHelper.checkDetailsToCreateSubmission(userAnswersWithNewEmail)

      result mustBe Right(
        PaperlessPreferenceSubmission(
          paperlessPreference = true,
          emailAddress = Some(newEmail),
          emailVerification = Some(true),
          bouncedEmail = Some(false)
        )
      )
    }

    "must return Right with submission when user has no existing subscription email" in {
      val newEmail = "new@example.com"

      val userAnswersNoSubscriptionEmail = userAnswersPostWithEmail.copy(
        subscriptionSummary = userAnswersPostWithEmail.subscriptionSummary.copy(
          emailAddress = None,
          paperlessReference = false
        ),
        emailAddress = Some(newEmail),
        verifiedEmailAddresses = Set(newEmail)
      )

      val result = testHelper.checkDetailsToCreateSubmission(userAnswersNoSubscriptionEmail)

      result mustBe Right(
        PaperlessPreferenceSubmission(
          paperlessPreference = true,
          emailAddress = Some(newEmail),
          emailVerification = Some(true),
          bouncedEmail = Some(false)
        )
      )
    }
  }

  "checkDetailsForPreferenceUpdatedPage" - {
    "must return a Right containing Some(email) if the user has submitted email as their preference" in {
      val result = testHelper.checkDetailsForPreferenceUpdatedPage(userAnswersPostWithEmail)

      result mustBe Right(Some(emailAddress))
    }

    "must return a Right containing None if the user has submitted post as their preference" in {
      val result = testHelper.checkDetailsForPreferenceUpdatedPage(userAnswers)

      result mustBe Right(None)
    }

    "must return a Left containing an ErrorModel if the contact preference in user answers is not set" in {
      val result = testHelper.checkDetailsForPreferenceUpdatedPage(emptyUserAnswers)

      result mustBe Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Contact preference updated but not found in user answers")
      )
    }

    "must return a Left containing an ErrorModel if the user has submitted email but no email address is found in user answers" in {
      val result = testHelper.checkDetailsForPreferenceUpdatedPage(userAnswersPostNoEmail.copy(emailAddress = None))

      result mustBe Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Contact preference updated to email but email not found in user answers")
      )
    }
    "checkDetailsForSameEmailSubmittedPage" - {
      "must return a Right with email when user is already on email, has selected email, and the emails match" in {
        val subscriptionEmail            = "existing@example.com"
        val userAnswersWithMatchingEmail = userAnswersPostWithEmail.copy(
          subscriptionSummary = userAnswersPostWithEmail.subscriptionSummary.copy(
            emailAddress = Some(subscriptionEmail),
            paperlessReference = true
          ),
          emailAddress = Some(subscriptionEmail)
        )

        val result = testHelper.checkDetailsForSameEmailSubmittedPage(userAnswersWithMatchingEmail)

        result mustBe Right(subscriptionEmail)
      }
    }
    "must return a Left when user is not on email" in {
      val userAnswers = emptyUserAnswers
        .set(ContactPreferencePage, true)
        .success
        .value
        .copy(subscriptionSummary = emptyUserAnswers.subscriptionSummary.copy(paperlessReference = false))

      testHelper.checkDetailsForSameEmailSubmittedPage(userAnswers) mustBe
        Left(ErrorModel(BAD_REQUEST, "User is not currently on email"))
    }

    "must return a Left when user has not selected email" in {
      val userAnswers = emptyUserAnswers
        .set(ContactPreferencePage, false)
        .success
        .value

      testHelper.checkDetailsForSameEmailSubmittedPage(userAnswers) mustBe
        Left(ErrorModel(BAD_REQUEST, "User has not selected email as contact preference"))
    }
    "must return a Left when emails don't match" in {
      val userAnswers = emptyUserAnswers
        .set(ContactPreferencePage, true)
        .success
        .value
        .copy(
          subscriptionSummary = emptyUserAnswers.subscriptionSummary.copy(
            emailAddress = Some("john.doe@example.com"),
            paperlessReference = true
          ),
          emailAddress = Some("jane.doe2@example.com")
        )

      testHelper.checkDetailsForSameEmailSubmittedPage(userAnswers) mustBe
        Left(ErrorModel(BAD_REQUEST, "Entered email and existing email do not match"))
    }
  }
}
