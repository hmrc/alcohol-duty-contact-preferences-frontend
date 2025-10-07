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
import connectors.SubmitPreferencesConnector
import controllers.routes
import models.audit.{Actions, ContactPreference, EmailVerificationOutcome, JourneyOutcome}
import models.{EmailVerificationDetails, ErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, EmailVerificationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.{PageCheckHelper, SummaryListHelper}
import views.html.changePreferences.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase {

  lazy val checkYourAnswersGetRoute: String =
    controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url

  lazy val checkYourAnswersPostRoute: String =
    controllers.changePreferences.routes.CheckYourAnswersController.onSubmit().url

  val mockAuditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
  }

  "CheckYourAnswersController" - {
    "onPageLoad" - {
      "must return OK and the correct view if no call to email verification is needed" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(dummySummaryList)(
            request,
            getMessages(application)
          ).toString

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswers))
          verify(summaryListHelper, times(1)).checkYourAnswersSummaryList(eqTo(userAnswers))(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }

      "must return OK and the correct view if a call to email verification is made and the email is verified" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = true, isLocked = false))

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(dummySummaryList)(
            request,
            getMessages(application)
          ).toString

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswersPostNoEmail))
          verify(summaryListHelper, times(1)).checkYourAnswersSummaryList(eqTo(userAnswersPostNoEmail))(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswersPostNoEmail))(any())
        }
      }

      "must redirect to the Email Locked Page if a call to email verification is made and the email is locked" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = true))

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.EmailLockedController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswersPostNoEmail))
          verify(summaryListHelper, times(0)).checkYourAnswersSummaryList(any())(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswersPostNoEmail))(any())
        }
      }

      "must redirect to Journey Recovery if a call to email verification is made and the email is neither verified nor locked" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = false))

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswersPostNoEmail))
          verify(summaryListHelper, times(0)).checkYourAnswersSummaryList(any())(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswersPostNoEmail))(any())
        }
      }

      "must redirect to Journey Recovery if user answers do not exist" in new SetUp {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(0)).checkDetailsForCheckYourAnswers(any())
          verify(summaryListHelper, times(0)).checkYourAnswersSummaryList(any())(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }

      "must redirect to Journey Recovery if PageCheckHelper returns an error when checking user answers" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersGetRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(emptyUserAnswers))
          verify(summaryListHelper, times(0)).checkYourAnswersSummaryList(any())(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }
    }

    "onSubmit" - {
      "must redirect to the Same Email Submitted page if user is updating their email and has submitted the same email" in new SetUp {
        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Left(
          ErrorModel(CONFLICT, "Email matches existing subscription")
        )

        val completeUserAnswers = userAnswers.copy(
          emailAddress = Some(emailAddress),
          subscriptionSummary = userAnswers.subscriptionSummary.copy(
            emailAddress = Some(emailAddress),
            paperlessReference = true
          )
        )

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.SameEmailSubmittedController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, never).submitContactPreferences(any(), any())(any())
          verify(mockAuditService, times(0)).audit(any())(any(), any())
        }
      }

      "must redirect to the Contact Preference Updated page and audit ChangeToEmail if submission is successful for post to email" in new SetUp {
        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = true,
          ContactPreference.Email.toString,
          Actions.ChangeToEmail.toString,
          Some(EmailVerificationOutcome(isVerified = true))
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionEmail
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.rightT(testSubmissionResponse)

        val completeUserAnswers = userAnswersPostWithEmail.copy(verifiedEmailAddresses = Set(emailAddress))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changePreferences.routes.PreferenceUpdatedController
            .onPageLoad()
            .url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, times(1))
            .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(appaId))(any())
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must redirect to the Contact Preference Updated page and audit ChangeToEmail if submission is successful for bounced scenario" in new SetUp {
        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = true,
          ContactPreference.Email.toString,
          Actions.ChangeToEmail.toString,
          Some(EmailVerificationOutcome(isVerified = true))
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionEmail
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.rightT(testSubmissionResponse)

        val completeUserAnswers = userAnswersPostWithBouncedEmail.copy(verifiedEmailAddresses = Set(emailAddress))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changePreferences.routes.PreferenceUpdatedController
            .onPageLoad()
            .url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, times(1))
            .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(appaId))(any())
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must redirect to the Contact Preference Updated page and audit ChangeToPost if submission is successful for email to post" in new SetUp {
        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = true,
          ContactPreference.Post.toString,
          Actions.ChangeToPost.toString,
          None
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionPost
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.rightT(testSubmissionResponse)

        val completeUserAnswers = userAnswers.copy(verifiedEmailAddresses = Set())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changePreferences.routes.PreferenceUpdatedController
            .onPageLoad()
            .url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, times(1))
            .submitContactPreferences(eqTo(contactPreferenceSubmissionPost), eqTo(appaId))(any())
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must redirect to the Contact Preference Updated page and audit AmendEmailAddress if submission is successful for amend email address" in new SetUp {
        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = true,
          ContactPreference.Email.toString,
          Actions.AmendEmailAddress.toString,
          Some(EmailVerificationOutcome(isVerified = true))
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionNewEmail
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.rightT(testSubmissionResponse)

        val completeUserAnswers = userAnswersEmailUpdate

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.changePreferences.routes.PreferenceUpdatedController
            .onPageLoad()
            .url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, times(1))
            .submitContactPreferences(eqTo(contactPreferenceSubmissionNewEmail), eqTo(appaId))(any())
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must redirect to Journey Recovery and audit ChangeToEmail if submission is not successful for post to email" in new SetUp {

        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = false,
          ContactPreference.Email.toString,
          Actions.ChangeToEmail.toString,
          Some(EmailVerificationOutcome(isVerified = true))
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionEmail
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.leftT(ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response"))

        val completeUserAnswers = userAnswersPostWithEmail.copy(verifiedEmailAddresses = Set(emailAddress))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(completeUserAnswers))
          verify(submitPreferencesConnector, times(1))
            .submitContactPreferences(eqTo(contactPreferenceSubmissionEmail), eqTo(appaId))(any())
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must audit unknown for an unknown scenario (post to post)" in new SetUp {
        val journeyOutcome: JourneyOutcome = JourneyOutcome(
          appaId,
          isSuccessful = true,
          ContactPreference.Post.toString,
          Actions.Unknown.toString,
          None
        )

        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Right(
          contactPreferenceSubmissionPost
        )
        when(submitPreferencesConnector.submitContactPreferences(any(), any())(any())) thenReturn
          EitherT.rightT(testSubmissionResponse)

        val completeUserAnswers = userAnswersPostNoEmail

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper), bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          verify(mockAuditService).audit(eqTo(journeyOutcome))(any(), any())
        }
      }

      "must redirect to Journey Recovery if user answers do not exist" in new SetUp {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(0)).checkDetailsForCheckYourAnswers(any())
          verify(submitPreferencesConnector, times(0)).submitContactPreferences(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery if PageCheckHelper returns an error when creating the submission" in new SetUp {
        when(pageCheckHelper.checkDetailsToCreateSubmission(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[SubmitPreferencesConnector].toInstance(submitPreferencesConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, checkYourAnswersPostRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsToCreateSubmission(eqTo(userAnswersPostWithEmail))
          verify(submitPreferencesConnector, times(0)).submitContactPreferences(any(), any())(any())
        }
      }

    }
  }

  class SetUp {
    val pageCheckHelper            = mock[PageCheckHelper]
    val summaryListHelper          = mock[SummaryListHelper]
    val emailVerificationService   = mock[EmailVerificationService]
    val submitPreferencesConnector = mock[SubmitPreferencesConnector]

    val row1             = SummaryListRow(key = Key(Text("Row1Key")), value = Value(Text("Row1Value")))
    val row2             = SummaryListRow(key = Key(Text("Row2Key")), value = Value(Text("Row2Value")))
    val dummySummaryList = SummaryList(rows = Seq(row1, row2))

    when(summaryListHelper.checkYourAnswersSummaryList(any())(any())) thenReturn dummySummaryList
  }
}
