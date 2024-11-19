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

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.mockito.MockitoSugar.mock
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{BAD_GATEWAY, CREATED, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class ContactPreferencesConnectorSpec extends SpecBase with ScalaFutures {
  "getContactPreferences" - {
    val mockUrl = s"http://alcohol-duty-contact-preferences/contactPreferences/$appaId"

    "successfully retrieve contact preferences" in new SetUp {
      val jsonResponse: String = Json.toJson(contactPreferencesResponse).toString()
      val httpResponse: HttpResponse = HttpResponse(OK, jsonResponse)

      when(mockConfig.adrGetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getContactPreferences(appaId)) { result =>
        result mustBe contactPreferencesResponse
        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when invalid JSON is returned" in new SetUp {
      val invalidJsonResponse: HttpResponse = HttpResponse(OK, """{ "invalid": "json" }""")

      when(mockConfig.adrGetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidJsonResponse)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getContactPreferences(appaId).failed) { e =>
        e.getMessage must include("Invalid JSON format")

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when an unexpected response is returned" in new SetUp {
      val upstreamErrorResponse: Future[Left[UpstreamErrorResponse, HttpResponse]] = Future.successful(
        Left[UpstreamErrorResponse, HttpResponse](UpstreamErrorResponse("", BAD_GATEWAY, BAD_GATEWAY, Map.empty))
      )

      when(mockConfig.adrGetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(upstreamErrorResponse)

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getContactPreferences(appaId).failed) { e =>
        e.getMessage must include("Unexpected response")

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when unexpected status code returned" in new SetUp {
      val invalidStatusCodeResponse: HttpResponse = HttpResponse(CREATED, "")

      when(mockConfig.adrGetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidStatusCodeResponse)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getContactPreferences(appaId).failed) { e =>
        e.getMessage mustBe "Unexpected status code: 201"

        verify(connector.httpClient, times(1))
          .get(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }
  }

  "submitReturn" - {
    val mockUrl = s"http://alcohol-duty-contact-preferences/update/contactPreferences/$appaId"

    "successfully submit a return" in new SetUp {
      val jsonResponse: String = Json.toJson(contactPreferencesResponse).toString()
      val httpResponse: HttpResponse = HttpResponse(OK, jsonResponse)

      when(mockConfig.adrSetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferencesRequest))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) { result =>
        result mustBe Right(contactPreferencesResponse)
        verify(connector.httpClient, times(1))
          .post(eqTo(url"$mockUrl"))(any())
      }
    }

    "fail when invalid JSON is returned" in new SetUp {
      val invalidJsonResponse: HttpResponse = HttpResponse(OK, """{ "invalid": "json" }""")

      when(mockConfig.adrSetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidJsonResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferencesRequest))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) { result =>
        result.swap.toOption.get must include("Invalid JSON format")

        verify(connector.httpClient, times(1))
          .post(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when an unexpected response is returned" in new SetUp {
      val upstreamErrorResponse: Future[Left[UpstreamErrorResponse, HttpResponse]] = Future.successful(
        Left[UpstreamErrorResponse, HttpResponse](UpstreamErrorResponse("", BAD_GATEWAY, BAD_GATEWAY, Map.empty))
      )

      when(mockConfig.adrSetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(upstreamErrorResponse)

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferencesRequest))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) { result =>
        result.swap.toOption.get must include("Unexpected response")

        verify(connector.httpClient, times(1))
          .post(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when unexpected status code returned" in new SetUp {
      val invalidStatusCodeResponse: HttpResponse = HttpResponse(UNPROCESSABLE_ENTITY, "")

      when(mockConfig.adrSetContactPreferencesUrl(eqTo(appaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidStatusCodeResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferencesRequest))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) { result =>
        result.swap.toOption.get must include("Unexpected status code: 422")

        verify(connector.httpClient, times(1))
          .post(eqTo(url"$mockUrl"))(any())

        verify(requestBuilder, times(1))
          .execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val connector                     = new ContactPreferencesConnector(config = mockConfig, httpClient = mock[HttpClientV2])

    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
