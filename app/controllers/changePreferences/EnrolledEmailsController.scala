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

import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.PageCheckHelper
import views.html.changePreferences.EnrolledEmailsView

import javax.inject.Inject

class EnrolledEmailsController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  helper: PageCheckHelper,
  val controllerComponents: MessagesControllerComponents,
  view: EnrolledEmailsView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    helper.checkDetailsForEnrolledEmailsPage(request.userAnswers) match {
      case Right(email) => Ok(view(email, appConfig.businessTaxAccountUrl))
      case Left(error)  =>
        logger.warn(s"[EnrolledEmailsController] [onPageLoad] ${error.message}")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
