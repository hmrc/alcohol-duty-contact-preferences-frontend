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

import models.{ErrorModel, UserAnswers}
import pages.changePreferences.ContactPreferencePage
import play.api.http.Status.BAD_REQUEST

import javax.inject.Inject

class ExistingEmailPageCheckHelper @Inject() {

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

}
