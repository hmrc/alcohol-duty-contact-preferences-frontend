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

import connectors.UserAnswersConnector
import controllers.actions._
import controllers.routes
import forms.EnterEmailAddressFormProvider
import models.requests.DataRequest
import models.{CheckMode, EmailVerificationDetails, Mode, NormalMode, UserAnswers, VerificationDetails}
import navigation.Navigator
import pages.changePreferences.EnterEmailAddressPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmailVerificationService
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
  userAnswersConnector: UserAnswersConnector,
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
          value =>
            for {
              newUserAnswers        <- newUserAnswers(request, value)
              addressEnteredDetails <- updateUserAnswersAndGetVerificationStatus(value, newUserAnswers)
            } yield Redirect(navigator.enterEmailAddressNavigation(addressEnteredDetails))
        )
  }

  private def updateUserAnswersAndGetVerificationStatus(value: String, newUserAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[EmailVerificationDetails] =
    if (newUserAnswers.verifiedEmailAddresses.contains(value)) {
      userAnswersConnector.set(newUserAnswers)
      Future.successful(EmailVerificationDetails(value, isVerified = true, isLocked = false))
    } else {
      emailVerificationService
        .retrieveAddressStatusAndAddToCache(VerificationDetails("TEST TEST"), value, newUserAnswers)
    }

  def newUserAnswers(request: DataRequest[AnyContent], newEmailAddress: String): Future[UserAnswers] =
    Future.successful(request.userAnswers.copy(emailAddress = Some(newEmailAddress)))

}
