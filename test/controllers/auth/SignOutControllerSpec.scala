/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.{FakeAppaId, FakeSignOutAction, SignOutAction}
import play.api.inject._
import play.api.test.Helpers._

import java.net.URLEncoder

class SignOutControllerSpec extends SpecBase {

  "signOut" - {

    "when user is authenticated, must redirect to sign out, specifying the exit survey as the continue URL" in {
      val application =
        applicationBuilder(None)
          .overrides(
            bind[FakeAppaId].toInstance(FakeAppaId(Some(appaId))),
            bind[SignOutAction].to[FakeSignOutAction]
          )
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request   = FakeRequestWithoutSession(GET, routes.SignOutController.signOut().url)

        val result = route(application, request).value

        val encodedContinueUrl  = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }

    "when user is not authenticated, must still redirect to sign out, specifying the exit survey as the continue URL" in {
      val application =
        applicationBuilder()
          .overrides(
            bind[FakeAppaId].toInstance(FakeAppaId(None)),
            bind[SignOutAction].to[FakeSignOutAction]
          )
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request   = FakeRequestWithoutSession(GET, routes.SignOutController.signOut().url)

        val result = route(application, request).value

        val encodedContinueUrl  = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }
}
