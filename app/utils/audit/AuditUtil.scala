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

package utils.audit

import models.UserAnswers
import models.audit.{ContactPreference, EmailVerificationOutcome, JourneyOutcome, JourneyStart}
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject

class AuditUtil @Inject() (auditService: AuditService) {

  def auditJourneyStartEvent(appaId: String, userAnswers: UserAnswers, action: String)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val currentPreference =
      if (userAnswers.subscriptionSummary.paperlessReference) {
        ContactPreference.Email.toString
      } else {
        ContactPreference.Post.toString
      }

    val journeyStart = JourneyStart(
      appaId,
      currentPreference,
      action
    )

    auditService.audit(journeyStart)
  }

  def auditJourneyOutcomeEvent(
    appaId: String,
    paperlessPreference: Boolean,
    contactPreferenceChange: String,
    isVerified: Option[Boolean],
    apiSuccess: Boolean
  )(implicit
    hc: HeaderCarrier
  ): Unit = {
    val newContactPreference =
      if (paperlessPreference) {
        ContactPreference.Email.toString
      } else {
        ContactPreference.Post.toString
      }

    val journeyOutcome = JourneyOutcome(
      alcoholDutyApprovalId = appaId,
      isSuccessful = apiSuccess,
      newContactPreference = newContactPreference,
      contactPreferenceChange = contactPreferenceChange,
      emailVerificationOutcome = isVerified.map(x => EmailVerificationOutcome(x))
    )

    auditService.audit(journeyOutcome)
  }

}
