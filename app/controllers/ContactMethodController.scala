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

package controllers

import connectors.UserAnswersConnector
import controllers.actions._
import forms.ContactMethodFormProvider
import models.{CheckMode, DecryptedSensitiveUserInformation, Mode, NormalMode, UserAnswers, UserDetails}
import navigation.Navigator
import pages.ContactMethodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContactMethodView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactMethodController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ContactMethodFormProvider,
  userAnswersConnector: UserAnswersConnector,
  val controllerComponents: MessagesControllerComponents,
  view: ContactMethodView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    mode match {
      case NormalMode =>
        for {
          _ <- userAnswersConnector.createUserAnswers(UserDetails(request.appaId, request.userId))
        } yield Ok(view(form, mode))
      case CheckMode  =>
        request.userAnswers match {
          case None     => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Some(ua) =>
            val preparedForm = ua.get(ContactMethodPage) match {
              case None        => form
              case Some(value) => form.fill(value)
            }
            Future.successful(Ok(view(preparedForm, mode)))
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
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactMethodPage, value))
              _              <- userAnswersConnector.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ContactMethodPage, mode, updatedAnswers))
        )
  }
}
