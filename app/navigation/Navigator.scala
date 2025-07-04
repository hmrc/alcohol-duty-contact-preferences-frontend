/*
 * Copyright 2024 HM Revenue & Customs
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

package navigation

import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import controllers.routes
import models._
import models.requests.DataRequest
import pages.changePreferences.{ContactPreferencePage, ExistingEmailPage}
import pages._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.StartEmailVerificationJourneyHelper

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Navigator @Inject() (
  emailVerificationConnector: EmailVerificationConnector,
  startJourneyHelper: StartEmailVerificationJourneyHelper,
  config: FrontendAppConfig
) extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ContactPreferencePage =>
      userAnswers => contactPreferenceRoute(userAnswers)
    case ExistingEmailPage     =>
      userAnswers =>
        userAnswers.get(ExistingEmailPage) match {
          case Some(true)  => routes.CheckYourAnswersController.onPageLoad()
          case Some(false) => controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode)
          case None        => routes.JourneyRecoveryController.onPageLoad()
        }
    case _                     =>
      logger.warn(
        "Navigation attempted from a page that doesn't exist in the route map in normal mode. Redirecting to journey recovery."
      )
      _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Boolean => Call = {
    case ContactPreferencePage =>
      userAnswers =>
        hasChanged =>
          if (hasChanged) contactPreferenceRoute(userAnswers)
          else routes.CheckYourAnswersController.onPageLoad()
    case _                     =>
      _ =>
        _ =>
          logger.warn(
            "Navigation attempted from a page that doesn't exist in the route map in check mode. Redirecting to Check Your Answers."
          )
          routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, hasAnswerChanged: Option[Boolean]): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)(hasAnswerChanged.getOrElse(false))
  }

  private def contactPreferenceRoute(userAnswers: UserAnswers): Call = {
    val selectedEmail      = userAnswers.get(ContactPreferencePage)
    val paperlessReference = userAnswers.subscriptionSummary.paperlessReference
    val hasVerifiedEmail   =
      userAnswers.subscriptionSummary.emailAddress.isDefined && userAnswers.subscriptionSummary.emailVerification
        .contains(true)

    (selectedEmail, paperlessReference, hasVerifiedEmail) match {
      case (Some(true), false, false) =>
        logger.info(
          "User selected email and is currently on post with no verified email in ETMP. Redirecting to /what-email-address"
        )
        controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode)
      case (Some(true), false, true)  =>
        logger.info(
          "User selected email and is currently on post but has a verified email in ETMP. Redirecting to /existing-email"
        )
        controllers.changePreferences.routes.ExistingEmailController.onPageLoad()
      case (Some(true), true, _)      =>
        logger.info("User selected email and is currently on email. Redirecting to /enrolled-emails")
        controllers.changePreferences.routes.EnrolledEmailsController.onPageLoad()
      case (Some(false), true, _)     =>
        logger.info("User selected post and is currently on email. Redirecting to Check Your Answers")
        routes.CheckYourAnswersController.onPageLoad()
      case (Some(false), false, _)    =>
        logger.info("User selected post and is currently on post. Redirecting to /enrolled-letters")
        controllers.changePreferences.routes.EnrolledLettersController.onPageLoad()
      case _                          =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }

  def enterEmailAddressNavigation(
    emailAddressEnteredDetails: EmailVerificationDetails,
    request: DataRequest[_]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    val (isVerified, isLocked) = (
      emailAddressEnteredDetails.isVerified,
      emailAddressEnteredDetails.isLocked
    )

    (isVerified, isLocked) match {
      case (true, _)      =>
        logger.info("User has a verified email address. Redirecting to Check Your Answers")
        Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
      case (false, true)  =>
        logger.info("User has been locked out for the entered email address.")
        Future.successful(Redirect(controllers.changePreferences.routes.EmailLockedController.onPageLoad()))
      case (false, false) =>
        handleEmailVerificationHandoff(request)
    }
  }

  private def handleEmailVerificationHandoff(
    request: DataRequest[_]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] =
    request.userAnswers.emailAddress match {
      case Some(emailAddress: String) =>
        emailVerificationConnector
          .startEmailVerification(startJourneyHelper.createRequest(request.credId, emailAddress))
          .value
          .map {
            case Right(redirectUri: RedirectUri) =>
              logger.info(s"Redirecting to Email Verification Service, uri: ${redirectUri.redirectUri}")
              val redirectTo = s"${config.emailVerificationRedirectBaseUrl}${redirectUri.redirectUri}"
              Redirect(redirectTo)
            case Left(errorModel: ErrorModel)    =>
              logger.info(
                s"Could not start email verification journey. message ${errorModel.message}, status: ${errorModel.status}"
              )
              Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
      case None                       =>
        logger.info("Unexpected error. No email address found in user answers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
}
