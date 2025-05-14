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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.EmailVerificationConnector
import models.ErrorModel
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json

class EmailVerificationConnectorISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = applicationBuilder(None)
    .configure(
      "microservice.services.alcohol-duty-contact-preferences.port" -> server.port(),
      "microservice.services.email-verification.port"               -> server.port()
    )
    .build()

  "EmailVerificationConnector" - {
    "getEmailVerification must" - {
      "successfully fetch verification details" in new SetUp {
        val jsonResponse: String = Json.toJson(testGetVerificationStatusResponse).toString()
        server.stubFor(
          get(urlMatching(getVerificationDetailsUrl))
            .willReturn(aResponse().withStatus(OK).withBody(jsonResponse))
        )

        whenReady(connector.getEmailVerification(testVerificationDetails).value) { result =>
          result mustBe Right(testGetVerificationStatusResponse)
        }
      }

      "return an error when the upstream service returns an error" in new SetUp {
        server.stubFor(
          get(urlMatching(getVerificationDetailsUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("test error"))
        )

        whenReady(connector.getEmailVerification(testVerificationDetails).value) { result =>
          result mustBe Left(
            ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response. Status: 500")
          )
        }
      }

      "return an error when the upstream response cannot be parsed as a redirect url" in new SetUp {
        server.stubFor(
          get(urlMatching(getVerificationDetailsUrl))
            .willReturn(aResponse().withStatus(OK).withBody("invalid body"))
        )

        whenReady(connector.getEmailVerification(testVerificationDetails).value) { result =>
          result mustBe Left(
            ErrorModel(
              INTERNAL_SERVER_ERROR,
              "Invalid JSON format. Could not parse response as GetVerificationStatusResponse"
            )
          )
        }
      }
    }

    "startEmailVerification must" - {
      "successfully fetch verification details" in new SetUp {
        server.stubFor(
          post(urlMatching(startEmailVerificationUrl))
            .willReturn(aResponse().withStatus(CREATED).withBody(testJsonRedirectUriString))
        )

        whenReady(connector.startEmailVerification(testEmailVerificationRequest).value) { result =>
          result mustBe Right(testRedirectUri)
        }
      }

      "return an error when the upstream service returns an error" in new SetUp {
        server.stubFor(
          post(urlMatching(startEmailVerificationUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("test error"))
        )

        whenReady(connector.startEmailVerification(testEmailVerificationRequest).value) { result =>
          result mustBe Left(
            ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response from email verification service. Http status: 500")
          )
        }
      }

      "return an error when the upstream response cannot be parsed as a redirect url" in new SetUp {
        server.stubFor(
          post(urlMatching(startEmailVerificationUrl))
            .willReturn(aResponse().withStatus(CREATED).withBody("invalid body"))
        )

        whenReady(connector.startEmailVerification(testEmailVerificationRequest).value) { result =>
          result mustBe Left(
            ErrorModel(
              INTERNAL_SERVER_ERROR,
              "Invalid JSON format, failed to parse response as a RedirectUrl"
            )
          )
        }
      }
    }
  }

  class SetUp {
    val connector: EmailVerificationConnector = app.injector.instanceOf[EmailVerificationConnector]
    val getVerificationDetailsUrl             = s"/alcohol-duty-contact-preferences/get-email-verification/$credId"
    val startEmailVerificationUrl             = s"/email-verification/verify-email"
  }
}
