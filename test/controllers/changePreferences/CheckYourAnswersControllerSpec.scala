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
import models.{EmailVerificationDetails, ErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EmailVerificationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.{CheckYourAnswersSummaryListHelper, PageCheckHelper}
import views.html.changePreferences.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase {

  lazy val checkYourAnswersRoute: String =
    controllers.changePreferences.routes.CheckYourAnswersController.onPageLoad().url

  "CheckYourAnswersController" - {
    "onPageLoad" - {
      "must return OK and the correct view if no call to email verification is needed" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(false)

        val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(dummySummaryList)(
            request,
            getMessages(application)
          ).toString

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswersPostNoEmail))
          verify(summaryListHelper, times(1)).createSummaryList(eqTo(userAnswersPostNoEmail))(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }

      "must return OK and the correct view if a call to email verification is made and the email is verified" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = true, isLocked = false))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(dummySummaryList)(
            request,
            getMessages(application)
          ).toString

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswers))
          verify(summaryListHelper, times(1)).createSummaryList(eqTo(userAnswers))(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswers))(any())
        }
      }

      "must redirect to the Email Locked Page if a call to email verification is made and the email is locked" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = true))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.changePreferences.routes.EmailLockedController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswers))
          verify(summaryListHelper, times(0)).createSummaryList(any())(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswers))(any())
        }
      }

      "must redirect to Journey Recovery if a call to email verification is made and the email is neither verified nor locked" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Right(true)
        when(emailVerificationService.retrieveAddressStatusAndAddToCache(any(), any(), any())(any())) thenReturn
          EitherT.rightT(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = false))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswers))
          verify(summaryListHelper, times(0)).createSummaryList(any())(any())
          verify(emailVerificationService, times(1))
            .retrieveAddressStatusAndAddToCache(any(), eqTo(emailAddress), eqTo(userAnswers))(any())
        }
      }

      "must redirect to Journey Recovery if user answers do not exist" in new SetUp {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(0)).checkDetailsForCheckYourAnswers(any())
          verify(summaryListHelper, times(0)).createSummaryList(any())(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }

      "must redirect to Journey Recovery if PageCheckHelper returns an error when checking user answers" in new SetUp {
        when(pageCheckHelper.checkDetailsForCheckYourAnswers(any())) thenReturn Left(
          ErrorModel(BAD_REQUEST, "Error from helper")
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
          .overrides(bind[PageCheckHelper].toInstance(pageCheckHelper))
          .overrides(bind[CheckYourAnswersSummaryListHelper].toInstance(summaryListHelper))
          .overrides(bind[EmailVerificationService].toInstance(emailVerificationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(pageCheckHelper, times(1)).checkDetailsForCheckYourAnswers(eqTo(userAnswersPostWithEmail))
          verify(summaryListHelper, times(0)).createSummaryList(any())(any())
          verify(emailVerificationService, times(0)).retrieveAddressStatusAndAddToCache(any(), any(), any())(any())
        }
      }
    }

    "onSubmit" - {}
  }

  class SetUp {
    val pageCheckHelper          = mock[PageCheckHelper]
    val summaryListHelper        = mock[CheckYourAnswersSummaryListHelper]
    val emailVerificationService = mock[EmailVerificationService]

    val row1             = SummaryListRow(key = Key(Text("Row1Key")), value = Value(Text("Row1Value")))
    val row2             = SummaryListRow(key = Key(Text("Row2Key")), value = Value(Text("Row2Value")))
    val dummySummaryList = SummaryList(rows = Seq(row1, row2))

    when(summaryListHelper.createSummaryList(any())(any())) thenReturn dummySummaryList
  }
}
