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

package services

import base.SpecBase
import models.ChangePreference
import models.audit._
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.Instant

class AuditServiceSpec extends SpecBase {

  private val mockAuditConnector = mock[AuditConnector]

  private val auditService = new AuditService(mockAuditConnector)

  "AuditService" - {

    "send JourneyStart correctly" in {
      val testDetail = JourneyStart(
        "someappaid",
        ContactPreference.Email.toString,
        ChangePreference.toString
      )
      auditService.audit(testDetail)

      verify(mockAuditConnector).sendExplicitAudit(eqTo("EmailContactPreferenceStart"), eqTo(testDetail))(any(), any(), any())
    }

    "send JourneyOutcome correctly" in {
      val testDetail = JourneyOutcome(
        "someappaid", isSuccessful = true,
        ContactPreference.Email.toString,
        Actions.ChangeToPost.toString,
        Some(EmailVerificationOutcome(isVerified = true, isLocked = false))
      )
      auditService.audit(testDetail)

      verify(mockAuditConnector).sendExplicitAudit(eqTo("EmailContactPreferenceOutcome"), eqTo(testDetail))(any(), any(), any())
    }

    "send EmailBounced correctly" in {
      val testDetail = EmailBounced(
        "someappaid",
        "test@test.com",
        "test bounce",
        Instant.now(clock)
      )
      auditService.audit(testDetail)

      verify(mockAuditConnector).sendExplicitAudit(eqTo("EmailContactPreferenceEmailBounced"), eqTo(testDetail))(any(), any(), any())
    }
  }
}
