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

package controllers.changePreferences

import cats.data.EitherT
import controllers.actions._
import controllers.routes
import forms.EnterEmailAddressFormProvider
import models.{CheckMode, EmailVerificationDetails, ErrorModel, Mode, NormalMode, UserAnswers, VerificationDetails}
import navigation.Navigator
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.changePreferences.EnterEmailAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterEmailAddressController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: EnterEmailAddressFormProvider,
  userAnswersService: UserAnswersService,
  emailVerificationService: EmailVerificationService,
  val controllerComponents: MessagesControllerComponents,
  view: EnterEmailAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      mode match {
        case NormalMode => Future.successful(Ok(view(form, mode)))
        case CheckMode  =>
          request.userAnswers.emailAddress match {
            case None        => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            case Some(value) => Future.successful(Ok(view(form.fill(value), mode)))
          }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value => {
            val updatedRequest = request.copy(userAnswers = request.userAnswers.copy(emailAddress = Some(value)))

            updateUserAnswersAndGetVerificationStatus(value, updatedRequest.userAnswers, request.credId).value.flatMap {
              case Left(error)                  =>
                logger.warn(
                  s"Failed to submit user's entered email address. message: ${error.message}, status: ${error.status}"
                )
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              case Right(addressEnteredDetails) =>
                navigator.enterEmailAddressNavigation(addressEnteredDetails, updatedRequest)
            }
          }
        )
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
