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
import config.Constants.submissionDetailsKey
import models.ErrorModel
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.PageCheckHelper
import views.html.changePreferences.PreferenceUpdatedView

// For Scala3
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}

class PreferenceUpdatedControllerSpec extends SpecBase {

  lazy val preferenceUpdatedRoute: String =
    controllers.changePreferences.routes.PreferenceUpdatedController.onPageLoad().url

  "PreferenceUpdatedController" - {

    "must return OK and the correct view for a GET when email preference has been submitted" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Right(Some(emailAddress))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, preferenceUpdatedRoute).withSession(
          submissionDetailsKey -> Json.toJson(testSubmissionResponse).toString()
        )

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreferenceUpdatedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(emailAddress), appConfig.businessTaxAccountUrl)(
          request,
          getMessages(application)
        ).toString

        verify(mockHelper, times(1)).checkDetailsForPreferenceUpdatedPage(eqTo(userAnswersPostNoEmail))
      }
    }

    "must return OK and the correct view for a GET when post preference has been submitted" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Right(None)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, preferenceUpdatedRoute).withSession(
          submissionDetailsKey -> Json.toJson(testSubmissionResponse).toString()
        )

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreferenceUpdatedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None, appConfig.businessTaxAccountUrl)(
          request,
          getMessages(application)
        ).toString

        verify(mockHelper, times(1)).checkDetailsForPreferenceUpdatedPage(eqTo(userAnswers))
      }
    }

    "must redirect to Journey Recovery if submissionDetailsKey is not present in the session" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Right(None)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequestWithoutSession(GET, preferenceUpdatedRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(0)).checkDetailsForPreferenceUpdatedPage(any())
      }
    }

    "must redirect to Journey Recovery if the value of submissionDetailsKey cannot be parsed as PaperlessPreferenceSubmittedResponse" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Right(None)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, preferenceUpdatedRoute).withSession(
          submissionDetailsKey -> Json.toJson("invalid").toString()
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(0)).checkDetailsForPreferenceUpdatedPage(any())
      }
    }

    "must redirect to Journey Recovery for a GET if user answers do not exist" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Error from helper")
      )

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, preferenceUpdatedRoute).withSession(
          submissionDetailsKey -> Json.toJson(testSubmissionResponse).toString()
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(0)).checkDetailsForPreferenceUpdatedPage(any())
      }
    }

    "must redirect to Journey Recovery for a GET if the helper returns an error when checking the user answers" in {
      val mockHelper = mock[PageCheckHelper]

      when(mockHelper.checkDetailsForPreferenceUpdatedPage(any())) thenReturn Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Error from helper")
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PageCheckHelper].toInstance(mockHelper))
        .build()

      running(application) {
        val request = FakeRequest(GET, preferenceUpdatedRoute).withSession(
          submissionDetailsKey -> Json.toJson(testSubmissionResponse).toString()
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockHelper, times(1)).checkDetailsForPreferenceUpdatedPage(eqTo(emptyUserAnswers))
      }
    }
  }
}
