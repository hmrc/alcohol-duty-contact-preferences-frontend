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

package controllers.changePreferences

import cats.data.EitherT
import controllers.actions._
import controllers.routes
import forms.BeforeYouStartFormProvider
import models.requests.DataRequest
import models.{BeforeYouStartAnswer, EmailVerificationDetails, ErrorModel, NormalMode, UserAnswers, VerificationDetails}
import navigation.Navigator
import pages.changePreferences.{ContactPreferencePage, ReturnPeriodKeyPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.changePreferences.BeforeYouStartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BeforeYouStartController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeforeYouStartFormProvider,
  userAnswersService: UserAnswersService,
  emailVerificationService: EmailVerificationService,
  val controllerComponents: MessagesControllerComponents,
  view: BeforeYouStartView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(ReturnPeriodKeyPage) match {
      case None    =>
        logger.warn("[BeforeYouStartController] [onPageLoad] No return period key found in user answers")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      case Some(_) =>
        val preparedForm = request.userAnswers.get(ContactPreferencePage) match {
          case Some(true)  =>
            form.fill(BeforeYouStartAnswer(contactPreferenceEmail = true, emailAddress = request.userAnswers.emailAddress))
          case Some(false) => form.fill(BeforeYouStartAnswer(contactPreferenceEmail = false, emailAddress = None))
          case None        => form
        }
        Ok(view(preparedForm))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(ReturnPeriodKeyPage) match {
      case None    =>
        logger.warn("[BeforeYouStartController] [onSubmit] No return period key found in user answers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(_) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
            {
              case BeforeYouStartAnswer(false, _)                 => continueByPost()
              case BeforeYouStartAnswer(true, Some(emailAddress)) => continueByEmail(emailAddress)
              case BeforeYouStartAnswer(true, None)                =>
                logger.warn("[BeforeYouStartController] [onSubmit] Email selected but no email address bound")
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
          )
    }
  }

  private def continueByPost()(implicit request: DataRequest[_]): Future[Result] =
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactPreferencePage, false))
      _              <- userAnswersService.set(updatedAnswers).value
    } yield Redirect(navigator.nextPage(ContactPreferencePage, NormalMode, updatedAnswers, None))

  private def continueByEmail(emailAddress: String)(implicit request: DataRequest[_]): Future[Result] = {
    val hasApprovedEmail =
      request.userAnswers.subscriptionSummary.emailAddress.exists(_.equalsIgnoreCase(emailAddress)) &&
        request.userAnswers.subscriptionSummary.emailVerification.contains(true)

    val userAnswersWithEmail = request.userAnswers.copy(emailAddress = Some(emailAddress))

    if (hasApprovedEmail) {
      for {
        updatedAnswers <- Future.fromTry(userAnswersWithEmail.set(ContactPreferencePage, true))
        _              <-
          userAnswersService.set(updatedAnswers.copy(verifiedEmailAddresses = updatedAnswers.verifiedEmailAddresses + emailAddress)).value
      } yield Redirect(controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad())
    } else {
      Future.fromTry(userAnswersWithEmail.set(ContactPreferencePage, true)).flatMap { updatedAnswers =>
        updateUserAnswersAndGetVerificationStatus(emailAddress, updatedAnswers, request.credId).value.flatMap {
          case Left(error)                  =>
            logger.warn(
              s"[BeforeYouStartController] [onSubmit] Failed to submit user's entered email address. status: ${error.status}"
            )
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Right(addressEnteredDetails) =>
            navigator.enterEmailAddressNavigation(addressEnteredDetails, request.copy(userAnswers = updatedAnswers))
        }
      }
    }
  }

  private def updateUserAnswersAndGetVerificationStatus(value: String, newUserAnswers: UserAnswers, credId: String)(
    implicit hc: HeaderCarrier
  ): EitherT[Future, ErrorModel, EmailVerificationDetails] =
    if (newUserAnswers.verifiedEmailAddresses.contains(value)) {
      for {
        _ <- userAnswersService.set(newUserAnswers)
      } yield EmailVerificationDetails(value, isVerified = true, isLocked = false)
    } else {
      emailVerificationService
        .retrieveAddressStatusAndAddToCache(VerificationDetails(credId = credId), value, newUserAnswers)
    }

}
