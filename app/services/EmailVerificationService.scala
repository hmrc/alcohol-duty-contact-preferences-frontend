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

package services

import cats.data.EitherT
import com.google.inject.Singleton
import connectors.EmailVerificationConnector
import models.{EmailVerificationDetails, ErrorModel, GetVerificationStatusResponse, UserAnswers, VerificationDetails}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
  emailVerificationConnector: EmailVerificationConnector,
  userAnswersService: UserAnswersService
)(implicit ec: ExecutionContext) {

  def retrieveAddressStatusAndAddToCache(
    verificationDetails: VerificationDetails,
    emailAddress: String,
    userAnswers: UserAnswers
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ErrorModel, EmailVerificationDetails] =
    for {
      successResponse    <- emailVerificationConnector.getEmailVerification(verificationDetails)
      verificationDetails = handleSuccess(emailAddress, successResponse)
      _                  <- addVerifiedToCache(verificationDetails, userAnswers)
    } yield verificationDetails

  private def handleSuccess(
    emailAddress: String,
    successResponse: GetVerificationStatusResponse
  ): EmailVerificationDetails = {

    val isEmailVerified: Boolean =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.verified)
    val isEmailLocked: Boolean   =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.locked)

    EmailVerificationDetails(emailAddress = emailAddress, isVerified = isEmailVerified, isLocked = isEmailLocked)

  }

  private def addVerifiedToCache(
    verificationDetails: EmailVerificationDetails,
    userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorModel, HttpResponse] = {
    val newVerifiedEmails: Set[String] =
      if (verificationDetails.isVerified) {
        userAnswers.verifiedEmailAddresses ++ Set(verificationDetails.emailAddress)
      } else {
        userAnswers.verifiedEmailAddresses
      }

    val newUserAnswers = userAnswers.copy(verifiedEmailAddresses = newVerifiedEmails)
    userAnswersService.set(newUserAnswers)
  }

}
