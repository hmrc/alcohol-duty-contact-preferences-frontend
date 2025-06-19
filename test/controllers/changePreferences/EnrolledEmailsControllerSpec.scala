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
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.PageCheckHelper
import views.html.changePreferences.EnrolledEmailsView

class EnrolledEmailsControllerSpec extends SpecBase {

  lazy val enrolledEmailsRoute: String = controllers.changePreferences.routes.EnrolledEmailsController.onPageLoad().url

  "EnrolledEmailsController" - {

    "must return OK and the correct view for a GET" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForEnrolledEmailsPage(any())) thenReturn Right(emailAddress)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, enrolledEmailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnrolledEmailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(emailAddress, appConfig.businessTaxAccountUrl)(
          request,
          getMessages(application)
        ).toString

        verify(mockHelper, times(1)).checkDetailsForEnrolledEmailsPage(eqTo(userAnswers))
      }
    }

    "must redirect to Journey Recovery for a GET if user answers do not exist" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForEnrolledEmailsPage(any())) thenReturn Left(
        ErrorModel(BAD_REQUEST, "Error from helper")
      )

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, enrolledEmailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(0)).checkDetailsForEnrolledEmailsPage(any())
      }
    }

    "must redirect to Journey Recovery for a GET if the helper returns an error when checking the user's details" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForEnrolledEmailsPage(any())) thenReturn Left(
        ErrorModel(BAD_REQUEST, "Error from helper")
      )

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, enrolledEmailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(1)).checkDetailsForEnrolledEmailsPage(eqTo(userAnswersPostNoEmail))
      }
    }
  }
}
