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
import connectors.SubmitPreferencesConnector
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json

class SubmitPreferencesConnectorISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = applicationBuilder(None)
    .configure("microservice.services.alcohol-duty-contact-preferences.port" -> server.port())
    .build()

  "SubmitPreferencesConnector" - {
    "submitContactPreferences" - {
      "must successfully submit contact preferences" in new SetUp {
        val jsonResponse = Json.toJson(testSubmissionResponse).toString()

        server.stubFor(
          put(urlMatching(submitPreferencesUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferenceSubmissionEmail))))
            .willReturn(aResponse().withStatus(OK).withBody(jsonResponse))
        )

        whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, appaId).value) {
          case Right(details) =>
            details.processingDate   mustBe testSubmissionResponse.processingDate
            details.formBundleNumber mustBe testSubmissionResponse.formBundleNumber
          case _              => fail("Test failed: result did not match expected value")
        }
      }

      "must fail when invalid JSON is returned" in new SetUp {
        val invalidJsonResponse = """{ "invalid": "json" }"""
        server.stubFor(
          put(urlMatching(submitPreferencesUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferenceSubmissionEmail))))
            .willReturn(aResponse().withStatus(OK).withBody(invalidJsonResponse))
        )

        whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, appaId).value) { result =>
          result.swap.toOption.get.status mustBe INTERNAL_SERVER_ERROR
          result.swap.toOption.get.message  must include("Invalid JSON format")
        }
      }

      "must fail when an error is returned" in new SetUp {
        server.stubFor(
          put(urlMatching(submitPreferencesUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferenceSubmissionEmail))))
            .willReturn(aResponse().withBody("upstreamErrorResponse").withStatus(BAD_GATEWAY))
        )

        recoverToExceptionIf[Exception] {
          connector.submitContactPreferences(contactPreferenceSubmissionEmail, appaId).value
        } map { ex =>
          ex.getMessage must include("Unexpected response")
        }
      }

      "must fail when an unexpected status code is returned" in new SetUp {
        server.stubFor(
          put(urlMatching(submitPreferencesUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferenceSubmissionEmail))))
            .willReturn(aResponse().withBody("invalidStatusCodeResponse").withStatus(CREATED))
        )
        recoverToExceptionIf[Exception] {
          connector.submitContactPreferences(contactPreferenceSubmissionEmail, appaId).value
        } map { ex =>
          ex.getMessage must include("Unexpected status code")
        }
      }
    }
  }

  class SetUp {
    val connector            = app.injector.instanceOf[SubmitPreferencesConnector]
    val submitPreferencesUrl = s"/alcohol-duty-contact-preferences/submit-preferences/$appaId"
  }
}
