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
import models.audit.{ContactPreference, JourneyStart}
import models.{BouncedEmail, ChangePreference, NormalMode, UpdateEmail}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import pages.changePreferences.ContactPreferencePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class ServiceEntryControllerSpec extends SpecBase {

  lazy val serviceEntryChangePreferenceRoute: String =
    controllers.routes.ServiceEntryController.createUserAnswersAndRedirect(ChangePreference).url
  lazy val serviceEntryUpdateEmailRoute: String      =
    controllers.routes.ServiceEntryController.createUserAnswersAndRedirect(UpdateEmail).url
  lazy val serviceEntryBouncedEmailRoute: String     =
    controllers.routes.ServiceEntryController.createUserAnswersAndRedirect(BouncedEmail).url

  val mockAuditService: AuditService = mock[AuditService]

  "ServiceEntryController" - {

    "createUserAnswersAndRedirect" - {
      "must create user answers and redirect to the Contact Preference page if EntryMode is ContactPreference" in new SetUp {
        val journeyStart: JourneyStart = JourneyStart(
          appaId,
          ContactPreference.Email.toString,
          ChangePreference.toString
        )

        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(emptyUserAnswers)
        )

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryChangePreferenceRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(NormalMode).url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
          verify(mockAuditService).audit(eqTo(journeyStart))(any(), any())
        }
      }

      "must create user answers and redirect to the Existing Email page if EntryMode is UpdateEmail and user is on email" in new SetUp {
        val journeyStart: JourneyStart = JourneyStart(
          appaId,
          ContactPreference.Email.toString,
          UpdateEmail.toString
        )

        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(emptyUserAnswers)
        )

        val userAnswersWithContactPreference = emptyUserAnswers.set(ContactPreferencePage, true).success.value

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryUpdateEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.ExistingEmailController.onPageLoad().url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(1)).set(eqTo(userAnswersWithContactPreference))(any())
          verify(mockAuditService).audit(eqTo(journeyStart))(any(), any())
        }
      }

      "must redirect to journey recovery if EntryMode is UpdateEmail but user is on post" in new SetUp {
        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(userAnswersPostNoEmail)
        )

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryUpdateEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
        }
      }

      "must create user answers and redirect to the Bounced Email page if EntryMode is BouncedEmail and user has a bounced email" in new SetUp {
        val journeyStart: JourneyStart = JourneyStart(
          appaId,
          ContactPreference.Post.toString,
          BouncedEmail.toString
        )

        val emptyUserAnswersWithBouncedEmail =
          emptyUserAnswers.copy(subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true)))

        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(emptyUserAnswersWithBouncedEmail)
        )

        val userAnswersWithContactPreference =
          emptyUserAnswersWithBouncedEmail.set(ContactPreferencePage, true).success.value

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryBouncedEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.EmailErrorController.onPageLoad().url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(1)).set(eqTo(userAnswersWithContactPreference))(any())
          verify(mockAuditService).audit(eqTo(journeyStart))(any(), any())
        }
      }

      "must redirect to journey recovery if EntryMode is BouncedEmail but user does not have a bounced email" in new SetUp {
        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Right(emptyUserAnswers)
        )

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryBouncedEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
        }
      }

      "must redirect to journey recovery if there is an error creating user answers" in new SetUp {
        when(mockUserAnswersConnector.createUserAnswers(any())(any())) thenReturn Future.successful(
          Left(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))
        )

        val application = applicationBuilder()
          .overrides(
            bind[UserAnswersConnector].toInstance(mockUserAnswersConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, serviceEntryChangePreferenceRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockUserAnswersConnector, times(1)).createUserAnswers(any())(any())
          verify(mockUserAnswersConnector, times(0)).set(any())(any())
        }
      }
    }
  }

  class SetUp {
    val mockUserAnswersConnector = mock[UserAnswersConnector]

    when(mockUserAnswersConnector.set(any())(any())) thenReturn Future.successful(mock[HttpResponse])
  }
}
