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

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

// For Scala3
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.*

import scala.concurrent.Future

class UserAnswersConnectorSpec extends SpecBase {
  "GET" - {
    "must successfully fetch user answers" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/user-answers/$appaId"
      when(mockConfig.ecpUserAnswersGetUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, UserAnswers]](any(), any()))
        .thenReturn(Future.successful(Right(userAnswers)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.get(appaId)) {
        _ mustBe Right(userAnswers)
      }
    }
  }

  "POST" - {
    "must successfully write user answers" in new SetUp {
      val postUrl = "http://alcohol-duty-contact-preferences/user-answers"

      when(mockConfig.ecpUserAnswersUrl()).thenReturn(postUrl)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.withBody(eqTo(Json.toJson(userDetails)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.createUserAnswers(userDetails)
      verify(connector.httpClient, atLeastOnce).post(eqTo(url"$postUrl"))(any())
    }
  }

  "PUT" - {
    "must successfully write user answers" in new SetUp {
      val putUrl = "http://alcohol-duty-contact-preferences/user-answers"

      when(mockConfig.ecpUserAnswersUrl()).thenReturn(putUrl)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.withBody(eqTo(Json.toJson(userAnswers)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.set(userAnswers)

      verify(connector.httpClient, atLeastOnce).put(eqTo(url"$putUrl"))(any())
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new UserAnswersConnector(config = mockConfig, httpClient = httpClient)
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
