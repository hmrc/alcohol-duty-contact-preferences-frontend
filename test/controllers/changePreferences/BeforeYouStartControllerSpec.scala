/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.BeforeYouStartFormProvider
import models.{BeforeYouStartAnswer, EmailVerificationDetails, ErrorModel}
import navigation.Navigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import pages.changePreferences.ReturnPeriodKeyPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HttpResponse
import views.html.changePreferences.BeforeYouStartView

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends SpecBase {

  lazy val beforeYouStartRoute: String =
    controllers.changePreferences.routes.BeforeYouStartController.onPageLoad().url

  val formProvider                     = new BeforeYouStartFormProvider()
  val form: Form[BeforeYouStartAnswer] = formProvider()

  val periodKey = "24AA"

  // has an approved, verified email already on the subscription (subscriptionSummaryPostWithEmail)
  val userAnswersForReturn        = userAnswersPostWithEmail.set(ReturnPeriodKeyPage, periodKey).success.value
  // no email on the subscription at all, and nothing cached as verified
  val userAnswersForReturnNoEmail = userAnswersPostNoEmail
    .copy(
      subscriptionSummary = subscriptionSummaryPostNoEmail,
      emailAddress = None,
      verifiedEmailAddresses = Set.empty,
      data = play.api.libs.json.Json.obj()
    )
    .set(ReturnPeriodKeyPage, periodKey)
    .success
    .value

  "BeforeYouStartController" - {

    "onPageLoad" - {
      "must return OK with the correct view when no previous answer exists" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail)).build()

        running(application) {
          val request = FakeRequest(GET, beforeYouStartRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[BeforeYouStartView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form)(request, getMessages(application)).toString
        }
      }

      "must populate the view correctly when the user has previously selected email" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersForReturn)).build()

        running(application) {
          val request = FakeRequest(GET, beforeYouStartRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[BeforeYouStartView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(BeforeYouStartAnswer(contactPreferenceEmail = true, emailAddress = Some(emailAddress)))
          )(request, getMessages(application)).toString
        }
      }

      "must redirect to Journey Recovery if user answers do not exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, beforeYouStartRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery if there is no return period key in user answers" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

        running(application) {
          val request = FakeRequest(GET, beforeYouStartRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must return a Bad Request and errors when no option is selected" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail)).build()

        running(application) {
          val request = FakeRequest(POST, beforeYouStartRoute).withFormUrlEncodedBody()

          val boundForm = form.bind(Map.empty[String, String])

          val view = application.injector.instanceOf[BeforeYouStartView]

          val result = route(application, request).value

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm)(request, getMessages(application)).toString
        }
      }

      "must return a Bad Request and errors when email is selected but no email address is provided" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail)).build()

        running(application) {
          val request =
            FakeRequest(POST, beforeYouStartRoute).withFormUrlEncodedBody(("contactPreferenceEmail", "true"))

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must redirect to Journey Recovery if there is no return period key in user answers" in {
        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

        running(application) {
          val request =
            FakeRequest(POST, beforeYouStartRoute).withFormUrlEncodedBody(("contactPreferenceEmail", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when post is selected" - {
        "must save the answer and follow the normal navigation to Enrolled Letters" in {
          val application = applicationBuilder(userAnswers = Some(userAnswersForReturn)).build()

          running(application) {
            val request =
              FakeRequest(POST, beforeYouStartRoute).withFormUrlEncodedBody(("contactPreferenceEmail", "false"))

            val result = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.changePreferences.routes.EnrolledLettersController.onPageLoad().url
          }
        }
      }

      "when email is selected" - {
        "and the entered email matches the approved, verified email already on the subscription" - {
          "must skip straight to Check Your Answers without calling the email verification service" in {
            val mockEmailVerificationService = mock[EmailVerificationService]

            val application = applicationBuilder(userAnswers = Some(userAnswersForReturn))
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            running(application) {
              val request = FakeRequest(POST, beforeYouStartRoute)
                .withFormUrlEncodedBody(("contactPreferenceEmail", "true"), ("emailAddress", emailAddress))

              val result = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url

              verify(mockEmailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(
                any()
              )
            }
          }
        }

        "and the entered email is different from the one on the subscription" - {
          "and it is already verified in the cache, must go via the cache-hit path" in {
            val mockUserAnswersService = mock[UserAnswersService]
            val mockNavigator          = mock[Navigator]

            when(mockUserAnswersService.set(any())(any())) thenReturn EitherT.rightT[Future, ErrorModel](
              HttpResponse(OK, "Test success")
            )
            when(mockNavigator.enterEmailAddressNavigation(any(), any())(any(), any(), any())) thenReturn Future
              .successful(Redirect(controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url))

            val userAnswersWithCachedEmail =
              userAnswersForReturn.copy(verifiedEmailAddresses = Set(emailAddress2))

            val application = applicationBuilder(userAnswers = Some(userAnswersWithCachedEmail))
              .overrides(
                bind[UserAnswersService].toInstance(mockUserAnswersService),
                bind[Navigator].toInstance(mockNavigator)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, beforeYouStartRoute)
                .withFormUrlEncodedBody(("contactPreferenceEmail", "true"), ("emailAddress", emailAddress2))

              val result = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual
                controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url

              verify(mockNavigator, times(1)).enterEmailAddressNavigation(any(), any())(any(), any(), any())
            }
          }

          "and it is not in the cache, must call the email verification service and follow the navigator" in {
            val mockEmailVerificationService = mock[EmailVerificationService]
            val mockNavigator                = mock[Navigator]

            when(
              mockEmailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
            ) thenReturn EitherT.rightT[Future, ErrorModel](
              EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = false)
            )
            when(mockNavigator.enterEmailAddressNavigation(any(), any())(any(), any(), any())) thenReturn Future
              .successful(Redirect("/email-verification-frontend"))

            val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail))
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[Navigator].toInstance(mockNavigator)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, beforeYouStartRoute)
                .withFormUrlEncodedBody(("contactPreferenceEmail", "true"), ("emailAddress", emailAddress2))

              val result = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual "/email-verification-frontend"

              verify(mockEmailVerificationService, times(1))
                .retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
              verify(mockNavigator, times(1)).enterEmailAddressNavigation(any(), any())(any(), any(), any())
            }
          }

          "must redirect to Journey Recovery if getting the verification details fails" in {
            val mockEmailVerificationService = mock[EmailVerificationService]

            when(
              mockEmailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
            ) thenReturn EitherT.leftT[Future, EmailVerificationDetails](
              ErrorModel(INTERNAL_SERVER_ERROR, "Test error")
            )

            val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail))
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .build()

            running(application) {
              val request = FakeRequest(POST, beforeYouStartRoute)
                .withFormUrlEncodedBody(("contactPreferenceEmail", "true"), ("emailAddress", emailAddress2))

              val result = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

        "and there is no approved email on the subscription at all" - {
          "must call the email verification service and follow the navigator" in {
            val mockEmailVerificationService = mock[EmailVerificationService]
            val mockNavigator                = mock[Navigator]

            when(
              mockEmailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
            ) thenReturn EitherT.rightT[Future, ErrorModel](
              EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = false)
            )
            when(mockNavigator.enterEmailAddressNavigation(any(), any())(any(), any(), any())) thenReturn Future
              .successful(Redirect("/email-verification-frontend"))

            val application = applicationBuilder(userAnswers = Some(userAnswersForReturnNoEmail))
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[Navigator].toInstance(mockNavigator)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, beforeYouStartRoute)
                .withFormUrlEncodedBody(("contactPreferenceEmail", "true"), ("emailAddress", emailAddress2))

              val result = route(application, request).value

              status(result)                 mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual "/email-verification-frontend"
            }
          }
        }
      }
    }
  }
}
