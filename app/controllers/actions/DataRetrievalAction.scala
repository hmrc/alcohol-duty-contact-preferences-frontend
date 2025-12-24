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

package controllers.actions

import connectors.UserAnswersConnector
import controllers.routes
import models.requests.{IdentifierRequest, OptionalDataRequest}
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  val userAnswersConnector: UserAnswersConnector
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, OptionalDataRequest[A]]] = {

    val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    userAnswersConnector
      .get(request.appaId)(headerCarrier)
      .map {
        case Right(ua)                              =>
          Right(
            OptionalDataRequest(
              request.request,
              request.appaId,
              request.groupId,
              request.userId,
              request.credId,
              Some(ua)
            )
          )
        case Left(ex) if ex.statusCode == NOT_FOUND =>
          logger.info(s"[DataRetrievalAction] [refine] User answers for ${request.appaId} not found")
          Right(
            OptionalDataRequest(
              request.request,
              request.appaId,
              request.groupId,
              request.userId,
              request.credId,
              None
            )
          )
        case Left(ex)                               =>
          logger.warn("[DataRetrievalAction] [refine] Data retrieval failed with exception: ", ex)
          Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}

trait DataRetrievalAction extends ActionRefiner[IdentifierRequest, OptionalDataRequest]
