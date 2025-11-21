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

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.changePreferences.EmailErrorView

class EmailErrorControllerSpec extends SpecBase {

  lazy val bouncedEmailRoute: String = controllers.changePreferences.routes.EmailErrorController.onPageLoad().url

  "EmailErrorController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithBouncedEmail)).build()

      running(application) {
        val request = FakeRequest(GET, bouncedEmailRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmailErrorView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view()(request, getMessages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if user answers do not exist" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, bouncedEmailRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the user does not have a bounced email" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail)).build()

      running(application) {
        val request = FakeRequest(GET, bouncedEmailRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
