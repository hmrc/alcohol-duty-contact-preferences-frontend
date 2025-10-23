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
import controllers.routes
import models.ErrorModel
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.PageCheckHelper
import views.html.changePreferences.SameEmailSubmittedView

// For Scala3
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}

class SameEmailSubmittedControllerSpec extends SpecBase {

  lazy val emailFoundRoute: String = controllers.changePreferences.routes.SameEmailSubmittedController.onPageLoad().url
  val testEmail                    = "test@example.com"

  "SameEmailSubmittedController" - {
    "must return OK and the correct view for a GET when emails match" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForSameEmailSubmittedPage(any())) thenReturn Right(testEmail)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, emailFoundRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[SameEmailSubmittedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(appConfig.businessTaxAccountUrl, testEmail)(
          request,
          messages(application)
        ).toString
        verify(mockHelper, times(1)).checkDetailsForSameEmailSubmittedPage(eqTo(emptyUserAnswers))
      }
    }

    "must redirect to Journey Recovery for a GET if user answers do not exist" in {
      val mockHelper = mock[PageCheckHelper]

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, emailFoundRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockHelper, never).checkDetailsForSameEmailSubmittedPage(any())
      }
    }

    "must redirect to Journey Recovery when emails don't match" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForSameEmailSubmittedPage(any()))
        .thenReturn(Left(ErrorModel(BAD_REQUEST, "Submitted email does not match subscription email")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, emailFoundRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockHelper, times(1)).checkDetailsForSameEmailSubmittedPage(eqTo(emptyUserAnswers))
      }
    }
  }
}
