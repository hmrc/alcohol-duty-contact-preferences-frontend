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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import pages._
import models._
import play.api.Logging

@Singleton
class Navigator @Inject() () extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case pages.ContactMethodPage =>
      userAnswers => contactMethodRoute(userAnswers, NormalMode)
    case _                       =>
      _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = { case _ =>
    _ => routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }

  private def contactMethodRoute(userAnswers: UserAnswers, mode: Mode): Call = {
    val selectedEmail      = userAnswers.get(pages.ContactMethodPage)
    val paperlessReference = userAnswers.paperlessReference
    val currentEmail       = userAnswers.decryptedSensitiveUserInformation.emailAddress
    (selectedEmail, paperlessReference, currentEmail) match {
      case (Some(true), false, None)    =>
        // TODO: next page is /what-email-address
        logger.info(
          "User selected email and is currently on post with no email in ETMP. Should redirect to /what-email-address"
        )
        routes.IndexController.onPageLoad()
      case (Some(true), false, Some(_)) =>
        // TODO: next page is /existing-email
        logger.info(
          "User selected email and is currently on post but has an email in ETMP. Should redirect to /existing-email"
        )
        routes.IndexController.onPageLoad()
      case (Some(true), true, Some(_))  =>
        // TODO: next page is /enrolled-emails
        logger.info("User selected email and is currently on email. Should redirect to /enrolled-emails")
        routes.IndexController.onPageLoad()
      case (Some(false), true, Some(_)) =>
        // TODO: next page is /check-answers
        logger.info("User selected post and is currently on email. Should redirect to /check-answers")
        routes.IndexController.onPageLoad()
      case (Some(false), false, _)      =>
        // TODO: next page is /enrolled-letters
        logger.info("User selected post and is currently on post. Should redirect to /enrolled-letters")
        routes.IndexController.onPageLoad()
      case _                            =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
