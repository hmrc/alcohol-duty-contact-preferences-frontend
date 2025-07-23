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

import cats.data.EitherT
import com.google.inject.Inject
import config.Constants.submissionDetailsKey
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.routes
import models._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmailVerificationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CheckYourAnswersSummaryListHelper, PageCheckHelper}
import views.html.changePreferences.CheckYourAnswersView

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  pageCheckHelper: PageCheckHelper,
  summaryListHelper: CheckYourAnswersSummaryListHelper,
  emailVerificationService: EmailVerificationService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    pageCheckHelper.checkDetailsForCheckYourAnswers(request.userAnswers) match {
      case Right(mustCheckVerificationStatus) =>
        if (mustCheckVerificationStatus) {
          val enteredEmail = request.userAnswers.emailAddress.getOrElse(
            throw new IllegalStateException(
              "Entered email address missing from user answers but not picked up by PageCheckHelper"
            )
          )
          emailVerificationService
            .retrieveAddressStatusAndAddToCache(VerificationDetails(request.credId), enteredEmail, request.userAnswers)
            .value
            .map {
              case Right(EmailVerificationDetails(_, isVerified, isLocked)) =>
                if (isVerified) {
                  val summaryList = summaryListHelper.createSummaryList(request.userAnswers)
                  Ok(view(summaryList))
                } else if (isLocked) {
                  Redirect(controllers.changePreferences.routes.EmailLockedController.onPageLoad())
                } else {
                  logger.warn("Entered email is neither verified nor locked")
                  Redirect(routes.JourneyRecoveryController.onPageLoad())
                }
              case Left(error)                                              =>
                logger.warn(s"Error checking email verification status. Status: ${error.status}")
                Redirect(routes.JourneyRecoveryController.onPageLoad())
            }
        } else {
          val summaryList = summaryListHelper.createSummaryList(request.userAnswers)
          Future.successful(Ok(view(summaryList)))
        }
      case Left(error)                        =>
        logger.warn(error.message)
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    pageCheckHelper.checkDetailsToCreateSubmission(request.userAnswers) match {
      case Right(contactPreferenceSubmission) =>
        // TODO: ADR-2151 - add connector and backend code to submit contact preferences, obtain response and delete user answers
        logger.debug(s"Created contact preference submission: ${Json.toJson(contactPreferenceSubmission)}")
        val submissionResponse = EitherT.rightT[Future, ErrorModel](
          PaperlessPreferenceSubmittedSuccess(PaperlessPreferenceSubmittedResponse(Instant.now(), "910000000000"))
        )

        submissionResponse.foldF(
          error => {
            logger.warn(error.message)
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          },
          submissionResponseDetails => {
            logger.info("Successfully submitted contact preferences")
            logger.debug(s"Submission response: $submissionResponseDetails")
            val session =
              request.session + (submissionDetailsKey -> Json.toJson(contactPreferenceSubmission).toString)
            Future.successful(
              Redirect(controllers.changePreferences.routes.PreferenceUpdatedController.onPageLoad())
                .withSession(session)
            )
          }
        )
      case Left(error)                        =>
        logger.warn(error.message)
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
