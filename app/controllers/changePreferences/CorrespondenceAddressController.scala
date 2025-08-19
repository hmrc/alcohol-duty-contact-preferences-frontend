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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.UserAnswers
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CheckYourAnswersSummaryListHelper, PageCheckHelper}
import viewmodels.govuk.summarylist._
import views.html.changePreferences.CorrespondenceAddressView

import javax.inject.Inject

class CorrespondenceAddressController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  pageCheckHelper: PageCheckHelper,
  summaryListHelper: CheckYourAnswersSummaryListHelper,
  val controllerComponents: MessagesControllerComponents,
  view: CorrespondenceAddressView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    pageCheckHelper.checkDetailsForCorrespondenceAddressPage(request.userAnswers) match {
      case Right(_)    =>
        val addressSummaryList = getCorrespondenceAddressSummaryList(request.userAnswers)
        Ok(view(addressSummaryList))
      case Left(error) =>
        logger.warn(error.message)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def getCorrespondenceAddressSummaryList(
    userAnswers: UserAnswers
  )(implicit messages: Messages): SummaryList = {
    val fullCorrespondenceAddress = summaryListHelper.getFullCorrespondenceAddress(userAnswers.subscriptionSummary)
    SummaryListViewModel(rows =
      Seq(
        SummaryListRowViewModel(
          key = KeyViewModel(HtmlContent(messages("checkYourAnswers.correspondenceAddress.key"))),
          value = ValueViewModel(HtmlContent(fullCorrespondenceAddress.replace("\n", "<br>")))
        )
      )
    )
  }
}
