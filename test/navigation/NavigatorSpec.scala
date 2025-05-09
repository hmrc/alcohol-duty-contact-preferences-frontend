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

import base.SpecBase
import connectors.EmailVerificationConnector
import controllers.routes
import pages._
import models._
import pages.changePreferences.ContactPreferencePage
import utils.StartEmailVerificationJourneyHelper

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator(
    emailVerificationConnector = mock[EmailVerificationConnector],
    startJourneyHelper = mock[StartEmailVerificationJourneyHelper],
    config = appConfig
  )

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "from the Contact Preference page" - {
        "must go to the What Email Address page if the user is currently on post, has selected email and has no email in ETMP" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostNoEmail.set(ContactPreferencePage, true).success.value
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must go to the Existing Email page if the user is currently on post, has selected email and has an email in ETMP" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostWithEmail.set(ContactPreferencePage, true).success.value
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must go to the Enrolled Emails page if the user is currently on email and has selected email" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswers.set(ContactPreferencePage, true).success.value
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must go to the Check Answers page if the user is currently on email and has selected post" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswers.set(ContactPreferencePage, false).success.value
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must go to the Enrolled Letters page if the user is currently on post and has selected post" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostNoEmail.set(ContactPreferencePage, false).success.value
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController
          .onPageLoad()
      }
    }
  }
}
