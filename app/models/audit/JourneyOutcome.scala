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

package models.audit

import models.audit.AuditType.EmailContactPreferenceOutcome
import play.api.libs.json.{Json, OFormat}

case class EmailVerificationOutcome(
  isVerified: Boolean
)

object EmailVerificationOutcome {
  implicit val format: OFormat[EmailVerificationOutcome] = Json.format[EmailVerificationOutcome]
}

case class JourneyOutcome(
  alcoholDutyApprovalId: String,
  isSuccessful: Boolean,
  newContactPreference: String,
  contactPreferenceChange: String,
  emailVerificationOutcome: Option[EmailVerificationOutcome]
) extends AuditEventDetail {
  protected val _auditType: AuditType = EmailContactPreferenceOutcome
}
object JourneyOutcome {
  implicit val format: OFormat[JourneyOutcome] = Json.format[JourneyOutcome]
}
