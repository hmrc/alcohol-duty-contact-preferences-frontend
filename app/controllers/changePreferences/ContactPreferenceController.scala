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
import forms.ContactPreferenceFormProvider
import models.{CheckMode, Mode, NormalMode, UserAnswers, UserDetails}
import navigation.Navigator
import pages.changePreferences.ContactPreferencePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.changePreferences.ContactPreferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactPreferenceController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ContactPreferenceFormProvider,
  userAnswersConnector: UserAnswersConnector,
  val controllerComponents: MessagesControllerComponents,
  view: ContactPreferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    mode match {
      case NormalMode =>
        request.userAnswers match {
          case None                  =>
            userAnswersConnector.createUserAnswers(UserDetails(request.appaId, request.userId)).map {
              case Right(_)    => Ok(view(form, mode))
              case Left(error) =>
                logger.warn(s"Error creating user answers: ${error.message}")
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
          case Some(existingAnswers) => Future.successful(Ok(view(getPreparedForm(existingAnswers), mode)))
        }
      case CheckMode  =>
        request.userAnswers.flatMap(_.get(ContactPreferencePage)) match {
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
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactPreferencePage, value))
              _              <- userAnswersConnector.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ContactPreferencePage, mode, updatedAnswers))
        )
  }

  private def getPreparedForm(ua: UserAnswers): Form[Boolean] = ua.get(ContactPreferencePage) match {
    case None        => form
    case Some(value) => form.fill(value)
  }
}
