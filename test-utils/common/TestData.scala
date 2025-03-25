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

package common

import config.Constants.ukTimeZoneStringId
import generators.ModelGenerators
import models._
import play.api.libs.json.{JsObject, Json}

import java.time._

trait TestData extends ModelGenerators {
  val clock           = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of(ukTimeZoneStringId))
  val appaId: String  = appaIdGen.sample.get
  val groupId: String = "groupid"
  val userId: String  = "user-id"

  val userDetails = UserDetails(appaId, userId)

  val emailAddress = "john.doe@example.com"

  val subscriptionSummaryEmail = SubscriptionSummary(
    paperlessReference = true,
    emailAddress = Some(emailAddress),
    emailVerification = Some(true),
    bouncedEmail = Some(false)
  )

  val subscriptionSummaryPostWithEmail = subscriptionSummaryEmail.copy(paperlessReference = false)

  val subscriptionSummaryPostNoEmail = subscriptionSummaryEmail.copy(paperlessReference = false)

  val userAnswers: UserAnswers = UserAnswers(
    appaId = appaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithEmail: UserAnswers = UserAnswers(
    appaId = appaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryPostWithEmail,
    emailAddress = None,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(false))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostNoEmail: UserAnswers = UserAnswers(
    appaId = appaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryPostNoEmail,
    emailAddress = None,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(false))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: UserAnswers = UserAnswers(
    appaId = appaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = Some(emailAddress),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )
}
