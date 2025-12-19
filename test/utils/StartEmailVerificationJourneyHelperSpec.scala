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

package utils

import base.SpecBase
import config.FrontendAppConfig
import models.{EmailModel, EmailVerificationRequest, Labels, LanguageInfo}
import play.api.i18n.{Lang, Messages}

import org.mockito.Mockito.*

class StartEmailVerificationJourneyHelperSpec extends SpecBase {

  override val testEmailVerificationRequest: EmailVerificationRequest = EmailVerificationRequest(
    credId = credId,
    continueUrl = "/test-continue",
    origin = "testOrigin",
    deskproServiceName = "alcohol-duty-returns-frontend",
    accessibilityStatementUrl = "/test-accessibility-url",
    backUrl = "/enter-email-address-url",
    email = EmailModel(address = emailAddress2, enterUrl = "/enter-email-address-url"),
    labels = Labels(
      LanguageInfo(pageTitle = "test-service-name", userFacingServiceName = "testOrigin"),
      LanguageInfo(pageTitle = "test-service-name", userFacingServiceName = "testOrigin")
    ),
    lang = "en",
    useNewGovUkServiceNavigation = true
  )

  "StartEmailVerificationJourneyHelper.createRequest" - {
    "must make a request when given a credId and the email a user has entered" in new SetUp {
      implicit val mockMessages: Messages = mock[Messages]

      when(mockConfig.startEmailVerificationContinueUrl).thenReturn("/test-continue")
      when(mockConfig.accessibilityStatementUrl).thenReturn("/test-accessibility-url")
      when(mockConfig.startEmailVerificationBackUrl).thenReturn("/enter-email-address-url")
      when(mockConfig.newServiceNavigationEnabled).thenReturn(true)
      when(mockMessages("emailVerificationJourney.signature")).thenReturn("testOrigin")
      when(mockMessages("service.name")).thenReturn("test-service-name")
      when(mockMessages.lang).thenReturn(Lang("en"))

      val result: EmailVerificationRequest = testHelper.createRequest(credId, emailAddress2)

      result mustBe testEmailVerificationRequest
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

    val testHelper: StartEmailVerificationJourneyHelper = new StartEmailVerificationJourneyHelper(mockConfig)
  }

}
