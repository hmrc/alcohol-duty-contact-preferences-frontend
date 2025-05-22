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
import models.NormalMode
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import pages.changePreferences.{ContactPreferencePage, ExistingEmailPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import views.html.changePreferences.ExistingEmailView

import scala.concurrent.Future

class ExistingEmailControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val existingEmailRoute: String = controllers.changePreferences.routes.ExistingEmailController.onPageLoad().url
  val formProvider                    = new ExistingEmailFormProvider()
  val form: Form[Boolean]             = formProvider(emailAddress)

  "ExistingEmailController" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail)).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ExistingEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, emailAddress)(
            request,
            getMessages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET if user answers already exist" in {
        val userAnswers = userAnswersPostWithEmail.set(ExistingEmailPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ExistingEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(false), emailAddress)(
            request,
            getMessages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the subscription summary has no email address" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the email in the subscription summary is not verified" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithUnverifiedEmail)).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the user has not selected email on the contact preference question" in {
        val userAnswers = userAnswersPostWithEmail.set(ContactPreferencePage, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted" in {
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ExistingEmailPage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, existingEmailRoute)
              .withFormUrlEncodedBody(("useExistingEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockUserAnswersConnector, times(1)).set(any())(any())
          verify(mockNavigator, times(1)).nextPage(eqTo(ExistingEmailPage), eqTo(NormalMode), any(), eqTo(None))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail)).build()

        running(application) {
          val request =
            FakeRequest(POST, existingEmailRoute)
              .withFormUrlEncodedBody(("useExistingEmail", ""))

          val boundForm = form.bind(Map("useExistingEmail" -> ""))

          val view = application.injector.instanceOf[ExistingEmailView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, emailAddress)(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, existingEmailRoute)
              .withFormUrlEncodedBody(("useExistingEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the subscription summary has no email address" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the email in the subscription summary is not verified" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithUnverifiedEmail)).build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the user has not selected email on the contact preference question" in {
        val userAnswers = userAnswersPostWithEmail.remove(ContactPreferencePage).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, existingEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
