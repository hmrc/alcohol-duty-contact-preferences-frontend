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

package testOnly.controllers

import com.google.inject.Inject
import connectors.UserAnswersConnector
import controllers.actions.IdentifierAction
import models.UserDetails
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.connectors.TestOnlyUserAnswersConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class TestOnlyController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  userAnswersConnector: UserAnswersConnector,
  testOnlyUserAnswersConnector: TestOnlyUserAnswersConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def clearAllData(): Action[AnyContent] = Action.async { implicit request =>
    testOnlyUserAnswersConnector.clearAllData().map(httpResponse => Ok(httpResponse.body))
  }

  def createAndShowUserAnswers(): Action[AnyContent] = identify.async { implicit request =>
    for {
      _  <- userAnswersConnector.createUserAnswers(UserDetails(request.appaId, request.userId))
      ua <- userAnswersConnector.get(request.appaId)
    } yield Ok(s"test successful, user answers: $ua")
  }
}
