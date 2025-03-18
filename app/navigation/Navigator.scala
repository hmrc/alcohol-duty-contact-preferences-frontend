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

@Singleton
class Navigator @Inject() () {

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
    val selectedEmail                = userAnswers.get(pages.ContactMethodPage)
    val paperlessReference           = true
    val currentEmail: Option[String] = Some("john.doe@example.com")
    (selectedEmail, paperlessReference, currentEmail) match {
      case (Some(true), false, None)    => // selected email, on post, no email in system
        routes.IndexController.onPageLoad()
//        controllers.adjustment.routes.SpoiltAlcoholicProductTypeController.onPageLoad(mode)
      // /what-email-address
      case (Some(true), false, Some(_)) => // selected email, on post, has email in system
        routes.IndexController.onPageLoad()
      // /existing-email
      case (Some(true), true, Some(_))  => // selected email, on email
        routes.IndexController.onPageLoad()
      // /enrolled-emails
      case (Some(false), true, Some(_)) => // selected paper, on email
        routes.IndexController.onPageLoad()
      // /check-answers
      case (Some(false), false, _)      => // selected paper, on paper
        routes.IndexController.onPageLoad()
      // /enrolled-letters
      case _                            =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
