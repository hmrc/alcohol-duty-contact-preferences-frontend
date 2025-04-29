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

package connectors

import cats.data.EitherT
import config.FrontendAppConfig
import models.{GetVerificationStatusResponse, UserAnswers, UserDetails, VerificationDetails}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EmailVerificationConnector @Inject() (
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getEmailVerification(verificationDetails: VerificationDetails)(implicit
    hc: HeaderCarrier
  ): Future[GetVerificationStatusResponse] =
    httpClient
      .get(url"${config.ecpGetEmailVerificationUrl(verificationDetails.credId)}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response)     =>
          Try(response.json.as[GetVerificationStatusResponse]) match {
            case Success(successResponse) => Future.successful(successResponse)
            case Failure(exception)       =>
              logger.warn(s"Invalid JSON format, failed to parse as GetVerificationStatusResponse", exception)
              Future.failed(new InternalError(s"Invalid JSON format $exception"))
          }
        case Left(errorResponse) =>
          logger.warn(
            s"Unexpected response when retrieving email verification details. Status: ${errorResponse.statusCode}, Message: ${errorResponse.message}"
          )
          Future.failed(new InternalError(s"Unexpected response. Status: ${errorResponse.statusCode}"))
      }

}
