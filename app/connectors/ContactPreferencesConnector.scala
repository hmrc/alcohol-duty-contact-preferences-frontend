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

package connectors

import cats.data.EitherT
import config.FrontendAppConfig
import models.preferences.ContactPreference
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ContactPreferencesConnector @Inject() (
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getContactPreference(appaId: String)(implicit hc: HeaderCarrier): Future[ContactPreference] =
    httpClient
      .get(url"${config.getContactPreferenceUrl(appaId)}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) if response.status == OK =>
          Try(response.json.as[ContactPreference]) match {
            case Success(data)      => throw new NotImplementedError("Not yet implemented!")
            case Failure(exception) => Future.failed(new Exception(s"Invalid JSON format $exception"))
          }
        case Left(errorResponse)                      => Future.failed(new Exception(s"Unexpected response: ${errorResponse.message}"))
        case Right(response)                          => Future.failed(new Exception(s"Unexpected status code: ${response.status}"))
      }

  def setContactPreference(appaId: String, contactPreferenceRequest: ContactPreference)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, String, ContactPreference] =
    EitherT {
      httpClient
        .post(url"${config.setContactPreferenceUrl(appaId)}")
        .withBody(
          Json.toJson(contactPreferenceRequest)
        )
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Right(response) if response.status == OK =>
            Try(response.json.as[ContactPreference]) match {
              case Success(data)      => throw new NotImplementedError("Not yet implemented!")
              case Failure(exception) =>
                logger.warn(s"Invalid JSON format", exception)
                Left(s"Invalid JSON format $exception")
            }
          case Left(errorResponse)                      =>
            logger.warn(s"Unable to update contact preference. Unexpected response: ${errorResponse.message}")
            Left(s"Unable to update contact preference. Unexpected response: ${errorResponse.message}")
          case Right(response)                          =>
            logger.warn(s"Unable to update contact preference. Unexpected status code: ${response.status}")
            Left(s"Unable to update contact preference. Unexpected status code: ${response.status}")
        }
    }
}
