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
import forms.ExistingEmailFormProvider
import models.NormalMode
import navigation.Navigator
import pages.changePreferences.ExistingEmailPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ExistingEmailPageCheckHelper
import views.html.changePreferences.ExistingEmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExistingEmailController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ExistingEmailFormProvider,
  userAnswersConnector: UserAnswersConnector,
  helper: ExistingEmailPageCheckHelper,
  val controllerComponents: MessagesControllerComponents,
  view: ExistingEmailView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    helper.checkDetailsForExistingEmailPage(request.userAnswers) match {
      case Right(email) =>
        val form         = formProvider(email)
        val preparedForm = request.userAnswers.get(ExistingEmailPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, email))
      case Left(error)  =>
        logger.warn(error.message)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    helper.checkDetailsForExistingEmailPage(request.userAnswers) match {
      case Right(email) =>
        val form = formProvider(email)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, email))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ExistingEmailPage, value))
                _              <- userAnswersConnector.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(ExistingEmailPage, NormalMode, updatedAnswers, None))
          )
      case Left(error)  =>
        logger.warn(error.message)
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
