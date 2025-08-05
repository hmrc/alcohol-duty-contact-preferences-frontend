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

import com.google.inject.Inject
import models.{CheckMode, UserAnswers}
import pages.changePreferences.ContactPreferencePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist._

class CheckYourAnswersSummaryListHelper @Inject() {

  def createSummaryList(userAnswers: UserAnswers)(implicit messages: Messages): SummaryList = {
    val contactPreferenceOption = userAnswers.get(ContactPreferencePage)
    val enteredEmailAddress     = userAnswers.emailAddress

    (contactPreferenceOption, enteredEmailAddress) match {
      case (Some(false), _)          =>
        SummaryListViewModel(rows =
          Seq(
            contactPreferenceRow(emailSelected = false),
            correspondenceAddressRow(userAnswers.subscriptionSummary.correspondenceAddress)
          )
        )
      case (Some(true), Some(email)) =>
        SummaryListViewModel(rows =
          Seq(
            contactPreferenceRow(emailSelected = true),
            emailAddressRow(email)
          )
        )
      case _                         =>
        throw new IllegalStateException(
          "User answers do not contain the required data but not picked up by PageCheckHelper"
        )
    }
  }

  private def contactPreferenceRow(emailSelected: Boolean)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = KeyViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.key"))),
      value = if (emailSelected) { ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.email"))) }
      else { ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.post"))) },
      actions = Seq(
        ActionItemViewModel(
          HtmlContent(messages("site.change")),
          controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url
        ).withVisuallyHiddenText(messages("checkYourAnswers.contactPreference.change.hidden"))
      )
    )

  private def emailAddressRow(emailAddress: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = KeyViewModel(HtmlContent(messages("checkYourAnswers.emailAddress.key"))),
      value = ValueViewModel(HtmlContent(emailAddress)),
      actions = Seq(
        ActionItemViewModel(
          HtmlContent(messages("site.change")),
          controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(CheckMode).url
        ).withVisuallyHiddenText(messages("checkYourAnswers.emailAddress.change.hidden"))
      )
    )

  private def correspondenceAddressRow(correspondenceAddress: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = KeyViewModel(HtmlContent(messages("checkYourAnswers.correspondenceAddress.key"))),
      value = ValueViewModel(HtmlContent(correspondenceAddress.replace("\n", "<br>")))
    )
}
