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
import forms.ContactPreferenceFormProvider
import models.{CheckMode, NormalMode}
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import pages.changePreferences.ContactPreferencePage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import views.html.changePreferences.ContactPreferenceView

import scala.concurrent.Future

class ContactPreferenceControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val contactPreferenceNormalRoute: String =
    controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(NormalMode).url
  lazy val contactPreferenceCheckRoute: String  =
    controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url

  val formProvider        = new ContactPreferenceFormProvider()
  val form: Form[Boolean] = formProvider()

  "ContactPreferenceController" - {

    "onPageLoad in normal mode" - {
      "must create user answers, then return OK and the correct view for a GET if user answers do not exist" in {
        val mockUserAnswersConnector = mock[UserAnswersConnector]

        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(userAnswersPostNoEmail)
        )

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, contactPreferenceNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactPreferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(
            request,
            getMessages(application)
          ).toString

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(eqTo(userDetails))(any())
        }
      }

      "must populate the view correctly on a GET if user answers already exist" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactPreferenceNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactPreferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode)(
            request,
            getMessages(application)
          ).toString
        }
      }
    }

    "onPageLoad in check mode" - {
      "must populate the view correctly on a GET" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactPreferenceCheckRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactPreferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), CheckMode)(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if user answers do not exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, contactPreferenceCheckRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the question has not been answered" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactPreferenceCheckRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted in normal mode" in {
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ContactPreferencePage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, contactPreferenceNormalRoute)
              .withFormUrlEncodedBody(("contactPreferenceEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockUserAnswersConnector, times(1)).set(any())(any())
          verify(mockNavigator, times(1)).nextPage(eqTo(ContactPreferencePage), eqTo(NormalMode), any(), eqTo(None))
        }
      }

      "must redirect to the next page when valid data is submitted in check mode and the answer has not changed" in {
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ContactPreferencePage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, contactPreferenceCheckRoute)
              .withFormUrlEncodedBody(("contactPreferenceEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockUserAnswersConnector, times(1)).set(any())(any())
          verify(mockNavigator, times(1))
            .nextPage(eqTo(ContactPreferencePage), eqTo(CheckMode), any(), eqTo(Some(false)))
        }
      }

      "must redirect to the next page when valid data is submitted in check mode and the answer has changed" in {
        val mockUserAnswersConnector = mock[UserAnswersConnector]
        val mockNavigator            = mock[Navigator]

        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
        when(mockNavigator.nextPage(eqTo(ContactPreferencePage), any(), any(), any())) thenReturn onwardRoute

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(mockNavigator),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, contactPreferenceCheckRoute)
              .withFormUrlEncodedBody(("contactPreferenceEmail", "false"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockUserAnswersConnector, times(1)).set(any())(any())
          verify(mockNavigator, times(1))
            .nextPage(eqTo(ContactPreferencePage), eqTo(CheckMode), any(), eqTo(Some(true)))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, contactPreferenceNormalRoute)
              .withFormUrlEncodedBody(("contactPreferenceEmail", ""))

          val boundForm = form.bind(Map("contactPreferenceEmail" -> ""))

          val view = application.injector.instanceOf[ContactPreferenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if user answers do not exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, contactPreferenceNormalRoute)
              .withFormUrlEncodedBody(("contactPreferenceEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
