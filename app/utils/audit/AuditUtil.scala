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

import models.audit._
import models.{PaperlessPreferenceSubmission, UserAnswers}
import play.api.Logging
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject

class AuditUtil @Inject() (auditService: AuditService) extends Logging {

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
    userAnswers: UserAnswers,
    contactPreferenceSubmission: PaperlessPreferenceSubmission,
    apiSuccess: Boolean
  )(implicit
    hc: HeaderCarrier
  ): Unit = {
    val newContactPreference =
      if (contactPreferenceSubmission.paperlessPreference) {
        ContactPreference.Email.toString
      } else {
        ContactPreference.Post.toString
      }

    val journeyOutcome = JourneyOutcome(
      alcoholDutyApprovalId = appaId,
      isSuccessful = apiSuccess,
      newContactPreference = newContactPreference,
      contactPreferenceChange = contactPreferenceChange(userAnswers, contactPreferenceSubmission),
      emailVerificationOutcome = contactPreferenceSubmission.emailVerification.map(x => EmailVerificationOutcome(x))
    )

    auditService.audit(journeyOutcome)
  }

  private def switchingToPost(
    userAnswers: UserAnswers,
    contactPreferenceSubmission: PaperlessPreferenceSubmission
  ): Boolean =
    userAnswers.subscriptionSummary.paperlessReference &&
      !contactPreferenceSubmission.paperlessPreference &&
      !userAnswers.subscriptionSummary.bouncedEmail.contains(true)

  private def switchingToEmail(
    userAnswers: UserAnswers,
    contactPreferenceSubmission: PaperlessPreferenceSubmission
  ): Boolean =
    !userAnswers.subscriptionSummary.paperlessReference &&
      contactPreferenceSubmission.paperlessPreference &&
      !userAnswers.subscriptionSummary.bouncedEmail.contains(true)

  private def amendingEmail(
    userAnswers: UserAnswers,
    contactPreferenceSubmission: PaperlessPreferenceSubmission
  ): Boolean = {
    val existingEmailPresent = userAnswers.subscriptionSummary.emailAddress.isDefined
    val updatedEmailPresent  = contactPreferenceSubmission.emailAddress.isDefined
    existingEmailPresent &&
    updatedEmailPresent &&
    userAnswers.subscriptionSummary.emailAddress.get != contactPreferenceSubmission.emailAddress.get
  }

  private def contactPreferenceChange(
    userAnswers: UserAnswers,
    contactPreferenceSubmission: PaperlessPreferenceSubmission
  ): String =
    if (switchingToPost(userAnswers, contactPreferenceSubmission)) {
      Actions.ChangeToPost.toString
    } else if (switchingToEmail(userAnswers, contactPreferenceSubmission)) {
      Actions.ChangeToEmail.toString
    } else if (amendingEmail(userAnswers, contactPreferenceSubmission)) {
      Actions.AmendEmailAddress.toString
    } else {
      logger.warn("Unknown user journey on contact preference submission")
      Actions.Unknown.toString
    }
}
