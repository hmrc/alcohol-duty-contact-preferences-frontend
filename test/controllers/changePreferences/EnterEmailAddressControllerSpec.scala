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
import cats.data.EitherT
import controllers.routes
import forms.EnterEmailAddressFormProvider
import models.{CheckMode, EmailVerificationDetails, ErrorModel, NormalMode}
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HttpResponse
import views.html.changePreferences.EnterEmailAddressView

// For Scala3
import org.mockito.Mockito.*

import scala.concurrent.Future

class EnterEmailAddressControllerSpec extends SpecBase {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val enterEmailAddressNormalRoute: String =
    controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode).url
  lazy val enterEmailAddressCheckRoute: String  =
    controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(CheckMode).url

  val formProvider       = new EnterEmailAddressFormProvider()
  val form: Form[String] = formProvider()

  "EnterEmailAddressController" - {

    "onPageLoad" - {
      "must return OK with the correct view in normal mode" in {

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailAddressNormalRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterEmailAddressView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(
            request,
            getMessages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET in check mode" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail)).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailAddressCheckRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterEmailAddressView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(emailAddress), CheckMode)(
            request,
            getMessages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if user answers do not contain an email address in check mode" in {
        val application =
          applicationBuilder(userAnswers = Some(userAnswersPostNoEmail.copy(emailAddress = None))).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailAddressCheckRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if user answers do not exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailAddressNormalRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if contact preference in user answers is not email" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailAddressNormalRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail)).build()

        running(application) {
          val request =
            FakeRequest(POST, enterEmailAddressNormalRoute)
              .withFormUrlEncodedBody(("emailAddress", ""))

          val boundForm = form.bind(Map("emailAddress" -> ""))

          val view = application.injector.instanceOf[EnterEmailAddressView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, getMessages(application)).toString
        }
      }

      "if entered email matches a verified email in the cache" - {
        "must redirect to Journey Recovery when the user answers set operation is unsuccessful" in {
          val mockUserAnswersService = mock[UserAnswersService]

          when(mockUserAnswersService.set(any())(any())) thenReturn EitherT.leftT[Future, HttpResponse](
            ErrorModel(INTERNAL_SERVER_ERROR, "Test error")
          )

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(verifiedEmailAddresses = Set(emailAddress2))))
              .overrides(
                bind[UserAnswersService].toInstance(mockUserAnswersService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, enterEmailAddressNormalRoute)
                .withFormUrlEncodedBody(("emailAddress", emailAddress2))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(mockUserAnswersService, times(1)).set(any())(any())
          }
        }

        "must go to Check Your Answers when the user answers set operation is successful" in {
          val mockUserAnswersService = mock[UserAnswersService]
          val mockNavigator          = mock[Navigator]

          when(mockUserAnswersService.set(any())(any())) thenReturn EitherT.rightT[Future, ErrorModel](
            HttpResponse(OK, "Test success")
          )
          when(mockNavigator.enterEmailAddressNavigation(any(), any())(any(), any(), any())) thenReturn Future
            .successful(Redirect(controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(verifiedEmailAddresses = Set(emailAddress2))))
              .overrides(
                bind[UserAnswersService].toInstance(mockUserAnswersService),
                bind[Navigator].toInstance(mockNavigator)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, enterEmailAddressNormalRoute)
                .withFormUrlEncodedBody(("emailAddress", emailAddress2))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url
            verify(mockUserAnswersService, times(1)).set(any())(any())
            verify(mockNavigator, times(1)).enterEmailAddressNavigation(any(), any())(any(), any(), any())
          }
        }
      }

      "if entered email doesn't match a verified email in the cache" - {
        "must redirect to Journey Recovery if there is an error getting the verification details" in {
          val mockEmailVerificationService = mock[EmailVerificationService]

          when(
            mockEmailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
          ) thenReturn EitherT.leftT[Future, EmailVerificationDetails](
            ErrorModel(INTERNAL_SERVER_ERROR, "Test error")
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[EmailVerificationService].toInstance(mockEmailVerificationService)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(POST, enterEmailAddressNormalRoute)
                .withFormUrlEncodedBody(("emailAddress", "TestEmail@email.com"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(mockEmailVerificationService, times(1)).retrieveAddressStatusAndAddToCache(any(), any(), any())(
              any()
            )
          }
        }

        "must redirect successfully if getting verification details is successful" in {
          val mockEmailVerificationService = mock[EmailVerificationService]
          val mockNavigator                = mock[Navigator]

          when(
            mockEmailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
          ) thenReturn EitherT.rightT[Future, ErrorModel](
            EmailVerificationDetails("TestEmail@email.com", isVerified = false, isLocked = false)
          )
          when(mockNavigator.enterEmailAddressNavigation(any(), any())(any(), any(), any())) thenReturn Future
            .successful(Redirect("/email-verification-frontend"))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[Navigator].toInstance(mockNavigator)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, enterEmailAddressNormalRoute)
                .withFormUrlEncodedBody(("emailAddress", "TestEmail@email.com"))
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual "/email-verification-frontend"
            verify(mockEmailVerificationService, times(1)).retrieveAddressStatusAndAddToCache(any(), any(), any())(
              any()
            )
            verify(mockNavigator, times(1)).enterEmailAddressNavigation(any(), any())(any(), any(), any())
          }
        }
      }
    }
  }
}
