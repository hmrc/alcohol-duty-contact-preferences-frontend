/*
 * Copyright 2024 HM Revenue & Customs
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

package navigation

import base.SpecBase
import cats.data.EitherT
import connectors.EmailVerificationConnector
import controllers.routes
import pages._
import models._
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import pages.changePreferences.{ContactPreferencePage, ExistingEmailPage}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.Messages
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import utils.StartEmailVerificationJourneyHelper

import scala.concurrent.Future

class NavigatorSpec extends SpecBase {

  val mockEmailVerificationConnector: EmailVerificationConnector  = mock[EmailVerificationConnector]
  val mockStartJourneyHelper: StartEmailVerificationJourneyHelper = mock[StartEmailVerificationJourneyHelper]

  val navigator = new Navigator(
    emailVerificationConnector = mockEmailVerificationConnector,
    startJourneyHelper = mockStartJourneyHelper,
    config = appConfig
  )

  "Navigator .nextPage" - {

    "in Normal mode" - {
      "must go from a page that doesn't exist in the route map to journey recovery" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers, None) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }

      "from the Contact Preference page" - {
        "must go to the What Email Address page if the user is currently on post, has selected email and has no email in ETMP" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostNoEmail.set(ContactPreferencePage, true).success.value,
            None
          ) mustBe controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode)
        }

        "must go to the Existing Email page if the user is currently on post, has selected email and has an email in ETMP" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostWithEmail.set(ContactPreferencePage, true).success.value,
            None
          ) mustBe controllers.changePreferences.routes.ExistingEmailController.onPageLoad()
        }

        "must go to the Enrolled Emails page if the user is currently on email and has selected email" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswers.set(ContactPreferencePage, true).success.value,
            None
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must go to the Check Your Answers page if the user is currently on email and has selected post" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswers.set(ContactPreferencePage, false).success.value,
            None
          ) mustBe routes.CheckYourAnswersController.onPageLoad()
        }

        "must go to the Enrolled Letters page if the user is currently on post and has selected post" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            userAnswersPostNoEmail.set(ContactPreferencePage, false).success.value,
            None
          ) mustBe routes.IndexController.onPageLoad()
          // TODO: change to correct route when page is created
        }

        "must redirect to journey recovery if the answer is missing" in {
          navigator.nextPage(
            ContactPreferencePage,
            NormalMode,
            emptyUserAnswers,
            None
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "from the Existing Email page" - {
        "must go to the Check Your Answers page if the user selects Yes" in {
          navigator.nextPage(
            ExistingEmailPage,
            NormalMode,
            userAnswersPostWithEmail.set(ExistingEmailPage, true).success.value,
            None
          ) mustBe routes.CheckYourAnswersController.onPageLoad()
        }

        "must go to the What Email Address page if the user selects No" in {
          navigator.nextPage(
            ExistingEmailPage,
            NormalMode,
            userAnswersPostWithEmail.set(ExistingEmailPage, false).success.value,
            None
          ) mustBe controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode)
        }

        "must redirect to journey recovery if the answer is missing" in {
          navigator.nextPage(
            ExistingEmailPage,
            NormalMode,
            userAnswersPostWithEmail,
            None
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "in Check mode" - {
      "must go from the Contact Preference page to the next page in normal mode if the answer has changed" in {
        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswersPostNoEmail.set(ContactPreferencePage, true).success.value,
          Some(true)
        ) mustBe controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(NormalMode)

        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswersPostWithEmail.set(ContactPreferencePage, true).success.value,
          Some(true)
        ) mustBe controllers.changePreferences.routes.ExistingEmailController.onPageLoad()

        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswers.set(ContactPreferencePage, true).success.value,
          Some(true)
        ) mustBe routes.IndexController.onPageLoad()
        // TODO: change to correct route when Enrolled Emails page is created

        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswers.set(ContactPreferencePage, false).success.value,
          Some(true)
        ) mustBe routes.CheckYourAnswersController.onPageLoad()

        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswersPostNoEmail.set(ContactPreferencePage, false).success.value,
          Some(true)
        ) mustBe routes.IndexController.onPageLoad()
        // TODO: change to correct route when Enrolled Letters page is created
      }

      "must go from the Contact Preference page to the Check Your Answers page if the answer has not changed" in {
        navigator.nextPage(
          ContactPreferencePage,
          CheckMode,
          userAnswers.set(ContactPreferencePage, true).success.value,
          Some(false)
        ) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers, Some(false)) mustBe routes.CheckYourAnswersController
          .onPageLoad()
      }
    }
  }

  "enterEmailAddressNavigation" - {
    "when the email provided is already verified then redirect to the check your answers page" in {
      implicit val mockMessages: Messages = mock[Messages]
      val dataRequest: DataRequest[_]     = mock[DataRequest[_]]

      val testEmailVerificationDetails = EmailVerificationDetails(emailAddress2, isVerified = true, isLocked = false)

      val result = navigator.enterEmailAddressNavigation(
        emailAddressEnteredDetails = testEmailVerificationDetails,
        request = dataRequest
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.CheckYourAnswersController.onPageLoad().url
    }

    "when the email provided is not verified, but is locked, then redirect to the check your answers page" in {
      implicit val mockMessages: Messages = mock[Messages]
      val dataRequest: DataRequest[_]     = mock[DataRequest[_]]

      val testEmailVerificationDetails = EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = true)

      val result = navigator.enterEmailAddressNavigation(
        emailAddressEnteredDetails = testEmailVerificationDetails,
        request = dataRequest
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.changePreferences.routes.EmailLockedController.onPageLoad().url
    }

    "when the email provided is not verified, and not locked" - {
      "and an email verification redirect url is obtained successfully then redirect there" in {
        implicit val mockMessages: Messages = mock[Messages]
        val mockDataRequest: DataRequest[_] = mock[DataRequest[_]]
        val testRedirectUri: RedirectUri    = RedirectUri(redirectUri = "/email-verification-frontend/test")

        when(mockDataRequest.userAnswers).thenReturn(userAnswers)
        when(mockEmailVerificationConnector.startEmailVerification(any())(any()))
          .thenReturn(EitherT.rightT[Future, ErrorModel](testRedirectUri))

        val testEmailVerificationDetails = EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = false)

        val result = navigator.enterEmailAddressNavigation(
          emailAddressEnteredDetails = testEmailVerificationDetails,
          request = mockDataRequest
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:9890/email-verification-frontend/test"
      }

      "and an email verification redirect url cannot be obtained then redirect to journey recovery" in {
        implicit val mockMessages: Messages = mock[Messages]
        val mockDataRequest: DataRequest[_] = mock[DataRequest[_]]

        when(mockDataRequest.userAnswers).thenReturn(userAnswers)
        when(mockEmailVerificationConnector.startEmailVerification(any())(any()))
          .thenReturn(EitherT.leftT[Future, RedirectUri](ErrorModel(INTERNAL_SERVER_ERROR, "test error")))

        val testEmailVerificationDetails = EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = false)

        val result = navigator.enterEmailAddressNavigation(
          emailAddressEnteredDetails = testEmailVerificationDetails,
          request = mockDataRequest
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "and no email address can be found in user answers then redirect to journey recovery" in {
        implicit val mockMessages: Messages = mock[Messages]
        val mockDataRequest: DataRequest[_] = mock[DataRequest[_]]

        when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

        val testEmailVerificationDetails = EmailVerificationDetails(emailAddress2, isVerified = false, isLocked = false)

        val result = navigator.enterEmailAddressNavigation(
          emailAddressEnteredDetails = testEmailVerificationDetails,
          request = mockDataRequest
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
