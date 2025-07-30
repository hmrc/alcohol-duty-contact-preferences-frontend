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

import models.{ErrorModel, PaperlessPreferenceSubmission, UserAnswers}
import pages.changePreferences.ContactPreferencePage
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}

import javax.inject.Inject

class PageCheckHelper @Inject() {

  def checkDetailsForEnrolledEmailsPage(userAnswers: UserAnswers): Either[ErrorModel, String] = {
    val isOnEmail                 = userAnswers.subscriptionSummary.paperlessReference
    val isEmailPreferenceSelected = userAnswers.get(ContactPreferencePage).contains(true)
    val existingEmail             = userAnswers.subscriptionSummary.emailAddress

    if (!isOnEmail) {
      Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User is currently on post."))
    } else if (!isEmailPreferenceSelected) {
      Left(ErrorModel(BAD_REQUEST, "Error on enrolled emails page: User has not selected email."))
    } else {
      existingEmail match {
        case None        =>
          Left(
            ErrorModel(
              INTERNAL_SERVER_ERROR,
              "Error on enrolled emails page: User is currently on email but has no email in subscription summary."
            )
          )
        case Some(email) => Right(email)
      }
    }
  }

  def checkDetailsForEnrolledLettersPage(userAnswers: UserAnswers): Either[ErrorModel, Unit] = {
    val isOnEmail                = userAnswers.subscriptionSummary.paperlessReference
    val isPostPreferenceSelected = userAnswers.get(ContactPreferencePage).contains(false)

    if (isOnEmail) {
      Left(ErrorModel(BAD_REQUEST, "Error on enrolled letters page: User is currently on email."))
    } else if (!isPostPreferenceSelected) {
      Left(ErrorModel(BAD_REQUEST, "Error on enrolled letters page: User has not selected post."))
    } else {
      Right((): Unit)
    }
  }

  def checkDetailsForExistingEmailPage(userAnswers: UserAnswers): Either[ErrorModel, String] = {
    val existingEmail             = userAnswers.subscriptionSummary.emailAddress
    val isExistingEmailVerified   = userAnswers.subscriptionSummary.emailVerification.contains(true)
    val isEmailPreferenceSelected = userAnswers.get(ContactPreferencePage).contains(true)

    existingEmail match {
      case None        =>
        Left(ErrorModel(BAD_REQUEST, "Error on existing email page: User has no email in subscription summary."))
      case Some(email) =>
        if (!isExistingEmailVerified) {
          Left(
            ErrorModel(
              BAD_REQUEST,
              "Error on existing email page: User's email in subscription summary is not verified."
            )
          )
        } else if (!isEmailPreferenceSelected) {
          Left(ErrorModel(BAD_REQUEST, "Error on existing email page: User has not selected email."))
        } else {
          Right(email)
        }
    }
  }

  def checkDetailsForCheckYourAnswers(userAnswers: UserAnswers): Either[ErrorModel, Boolean] = {
    val contactPreferenceOption = userAnswers.get(ContactPreferencePage)
    val enteredEmailAddress     = userAnswers.emailAddress

    (contactPreferenceOption, enteredEmailAddress) match {
      case (Some(false), _)          => Right(false)
      case (Some(true), Some(email)) =>
        if (userAnswers.verifiedEmailAddresses.contains(email)) Right(false) else Right(true)
      case _                         =>
        Left(ErrorModel(BAD_REQUEST, "Error on Check Your Answers: User answers do not contain the required data."))
    }
  }

  def checkDetailsToCreateSubmission(userAnswers: UserAnswers): Either[ErrorModel, PaperlessPreferenceSubmission] = {
    val contactPreferenceOption = userAnswers.get(ContactPreferencePage)
    val enteredEmailAddress     = userAnswers.emailAddress

    (contactPreferenceOption, enteredEmailAddress) match {
      case (Some(false), _)          =>
        Right(
          PaperlessPreferenceSubmission(
            paperlessPreference = false,
            emailAddress = None,
            emailVerification = None,
            bouncedEmail = None
          )
        )
      case (Some(true), Some(email)) =>
        if (userAnswers.verifiedEmailAddresses.contains(email)) {
          Right(
            PaperlessPreferenceSubmission(
              paperlessPreference = true,
              emailAddress = Some(email),
              emailVerification = Some(true),
              bouncedEmail = Some(false)
            )
          )
        } else {
          Left(ErrorModel(BAD_REQUEST, "Error creating submission: Email address is not verified."))
        }
      case _                         =>
        Left(ErrorModel(BAD_REQUEST, "Error creating submission: User answers do not contain the required data."))
    }
  }
}
