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
import connectors.UserAnswersConnector
import controllers.routes
import forms.ExistingEmailFormProvider
import models.{ErrorModel, NormalMode}
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import pages.changePreferences.ExistingEmailPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import utils.PageCheckHelper
import views.html.changePreferences.ExistingEmailView


import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.*

import scala.concurrent.Future

class ExistingEmailControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val existingEmailRoute: String = controllers.changePreferences.routes.ExistingEmailController.onPageLoad().url
  val formProvider                    = new ExistingEmailFormProvider()
  val form: Form[Boolean]             = formProvider(emailAddress)

  "ExistingEmailController" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        val mockHelper = mock[PageCheckHelper]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Right(emailAddress)

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(bind[PageCheckHelper].toInstance(mockHelper))
          .build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ExistingEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, emailAddress)(
            request,
            getMessages(application)
          ).toString

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswersPostWithEmail))
        }
      }

      "must populate the view correctly on a GET if the question has already been answered" in {
        val mockHelper = mock[PageCheckHelper]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Right(emailAddress)

        val userAnswers = userAnswersPostWithEmail.set(ExistingEmailPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PageCheckHelper].toInstance(mockHelper))
          .build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ExistingEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(false), emailAddress)(
            request,
            getMessages(application)
          ).toString

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswers))
        }
      }

      "must redirect to Journey Recovery for a GET if user answers do not exist" in {
        val mockHelper = mock[PageCheckHelper]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[PageCheckHelper].toInstance(mockHelper))
          .build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockHelper, times(0)).checkDetailsForExistingEmailPage(any())
        }
      }

      "must redirect to Journey Recovery for a GET if the helper returns an error when checking the user's details" in {
        val mockHelper = mock[PageCheckHelper]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(bind[PageCheckHelper].toInstance(mockHelper))
          .build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswersPostNoEmail))
        }
      }
    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted" in {
        val mockHelper               = mock[PageCheckHelper]
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Right(emailAddress)
        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ExistingEmailPage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(
            bind[PageCheckHelper].toInstance(mockHelper),
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)
            .withFormUrlEncodedBody(("useExistingEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswersPostWithEmail))
          verify(mockUserAnswersConnector, times(1)).set(any())(any())
          verify(mockNavigator, times(1)).nextPage(eqTo(ExistingEmailPage), eqTo(NormalMode), any(), eqTo(None))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val mockHelper               = mock[PageCheckHelper]
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Right(emailAddress)
        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ExistingEmailPage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(
            bind[PageCheckHelper].toInstance(mockHelper),
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)
            .withFormUrlEncodedBody(("useExistingEmail", ""))

          val boundForm = form.bind(Map("useExistingEmail" -> ""))

          val view = application.injector.instanceOf[ExistingEmailView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, emailAddress)(request, getMessages(application)).toString

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswersPostWithEmail))
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
          verify(mockNavigator, times(0)).nextPage(any(), any(), any(), any())
        }
      }

      "must redirect to Journey Recovery for a POST if user answers do not exist" in {
        val mockHelper               = mock[PageCheckHelper]
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )
        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ExistingEmailPage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[PageCheckHelper].toInstance(mockHelper),
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)
            .withFormUrlEncodedBody(("useExistingEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockHelper, times(0)).checkDetailsForExistingEmailPage(any())
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
          verify(mockNavigator, times(0)).nextPage(any(), any(), any(), any())
        }
      }

      "must redirect to Journey Recovery for a POST if the subscription summary has no email address" in {
        val mockHelper               = mock[PageCheckHelper]
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockHelper.checkDetailsForExistingEmailPage(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )
        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ExistingEmailPage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(
            bind[PageCheckHelper].toInstance(mockHelper),
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockHelper, times(1)).checkDetailsForExistingEmailPage(eqTo(userAnswersPostNoEmail))
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
          verify(mockNavigator, times(0)).nextPage(any(), any(), any(), any())
        }
      }
    }
  }
}
