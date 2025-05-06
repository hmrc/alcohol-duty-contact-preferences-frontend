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
      successResponse <- emailVerificationConnector.getEmailVerification(verificationDetails)
      _               <- addVerifiedToCache(successResponse, userAnswers)
    } yield handleSuccess(emailAddress, successResponse)

  private def handleSuccess(
    emailAddress: String,
    successResponse: GetVerificationStatusResponse
  ): EmailVerificationDetails =
    successResponse.emails.find(_.emailAddress == emailAddress) match {
      case Some(matchingEmail) =>
        EmailVerificationDetails(
          emailAddress = matchingEmail.emailAddress,
          isVerified = matchingEmail.verified,
          isLocked = matchingEmail.locked
        )
      case None                => EmailVerificationDetails(emailAddress = emailAddress, isVerified = false, isLocked = false)
    }

  private def addVerifiedToCache(
    emailVerificationResponse: GetVerificationStatusResponse,
    userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): EitherT[Future, ErrorModel, HttpResponse] = {
    val newVerifiedEmails = userAnswers.verifiedEmailAddresses ++ emailVerificationResponse.emails
      .filter(item => item.verified)
      .map(_.emailAddress)
      .toSet
    val newUserAnswers    = userAnswers.copy(verifiedEmailAddresses = newVerifiedEmails)
    userAnswersService.set(newUserAnswers)
  }

}
