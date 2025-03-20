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

package controllers

import base.SpecBase
import connectors.UserAnswersConnector
import forms.ContactMethodFormProvider
import models.{CheckMode, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import pages.ContactMethodPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import views.html.ContactMethodView

import scala.concurrent.Future

class ContactMethodControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  lazy val contactMethodNormalRoute = routes.ContactMethodController.onPageLoad(NormalMode).url
  lazy val contactMethodCheckRoute  = routes.ContactMethodController.onPageLoad(CheckMode).url

  val formProvider = new ContactMethodFormProvider()
  val form         = formProvider()

  val mockUserAnswersConnector = mock[UserAnswersConnector]
  val mockHttpResponse         = mock[HttpResponse]

  "ContactMethodController" - {

    "onPageLoad in normal mode" - {
      "must create user answers, then return OK and the correct view for a GET if user answers do not exist" - {
        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(emptyUserAnswers)
        )

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, contactMethodNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactMethodView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, getMessages(application)).toString
        }
      }

      "must return OK and the correct view for a GET if user answers already exist and the question has not previously been answered" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactMethodNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactMethodView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, getMessages(application)).toString
        }
      }

      "must populate the view correctly on a GET if user answers already exist and the question has previously been answered" in {
        val userAnswers = emptyUserAnswers.set(ContactMethodPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactMethodNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactMethodView]

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
        val userAnswers = emptyUserAnswers.set(ContactMethodPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactMethodCheckRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContactMethodView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), CheckMode)(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, contactMethodCheckRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the question has not been answered" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactMethodCheckRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted" in {
        when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mockHttpResponse)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, contactMethodNormalRoute)
              .withFormUrlEncodedBody(("contactMethodEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, contactMethodNormalRoute)
              .withFormUrlEncodedBody(("contactMethodEmail", ""))

          val boundForm = form.bind(Map("contactMethodEmail" -> ""))

          val view = application.injector.instanceOf[ContactMethodView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, contactMethodNormalRoute)
              .withFormUrlEncodedBody(("contactMethodEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
