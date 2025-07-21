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
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._

class CheckYourAnswersSummaryListHelperSpec extends SpecBase {

  implicit val messages: Messages = getMessages(app)

  val summaryListHelper = new CheckYourAnswersSummaryListHelper

  val contactPreferenceRowEmail = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.key"))),
    value = ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.email"))),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.contactPreference.change.hidden"))
    )
  )

  val contactPreferenceRowPost = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.key"))),
    value = ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.post"))),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.contactPreference.change.hidden"))
    )
  )

  val emailAddressRow = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.emailAddress.key"))),
    value = ValueViewModel(HtmlContent(emailAddress)),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.emailAddress.change.hidden"))
    )
  )

  val correspondenceAddressRow = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.correspondenceAddress.key"))),
    value = ValueViewModel(HtmlContent("")), // TODO: Add correspondence address
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.CorrespondenceAddressController.onPageLoad().url
      ).withVisuallyHiddenText(messages("checkYourAnswers.correspondenceAddress.change.hidden"))
    )
  )

  "CheckYourAnswersSummaryListHelper" - {
    "must return a summary list with the correct rows if email is selected" in {
      val summaryList = summaryListHelper.createSummaryList(userAnswers)

      summaryList mustBe SummaryListViewModel(rows = Seq(contactPreferenceRowEmail, emailAddressRow))
    }

    "must return a summary list with the correct rows if post is selected" in {
      val summaryList = summaryListHelper.createSummaryList(userAnswersPostNoEmail)

      summaryList mustBe SummaryListViewModel(rows = Seq(contactPreferenceRowPost, correspondenceAddressRow))
    }

    "must throw an exception if no contact preference is selected" in {
      val exception = intercept[RuntimeException] {
        summaryListHelper.createSummaryList(emptyUserAnswers)
      }
      exception.getMessage mustBe "User answers do not contain the required data but not picked up by PageCheckHelper"
    }

    "must throw an exception if email is selected but no email address is provided" in {
      val exception = intercept[RuntimeException] {
        summaryListHelper.createSummaryList(userAnswersPostWithEmail)
      }
      exception.getMessage mustBe "User answers do not contain the required data but not picked up by PageCheckHelper"
    }
  }
}
