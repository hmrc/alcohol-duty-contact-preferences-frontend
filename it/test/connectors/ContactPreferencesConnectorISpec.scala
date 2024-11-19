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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.preferences.ContactPreferences
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.Application
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, OK}
import play.api.libs.json.Json
import org.scalatest.RecoverMethods.recoverToExceptionIf

class ContactPreferencesConnectorISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = applicationBuilder(None)
    .configure("microservice.services.alcohol-duty-contact-preferences.port" -> server.port())
    .build()

  "ContactPreferencesConnector" - {
    "getContactPreferences" - {
      "should successfully retrieve contact preferences" in new SetUp {
        val contactPreferencesResponse = new ContactPreferences("1", None, None, None)
        val jsonResponse: String       =
          Json
            .toJson(contactPreferencesResponse)
            .toString() // TODO Return the appropriate response when we have the API spec

        server.stubFor(
          get(urlMatching(getUrl))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(jsonResponse)
            )
        )

        whenReady(connector.getContactPreferences(appaId)) { result =>
          result mustBe contactPreferencesResponse
        }
      }

      "should fail when invalid JSON is returned" in new SetUp {
        val invalidJsonResponse = """{ "invalid": "json" }"""
        server.stubFor(
          get(urlMatching(getUrl))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(invalidJsonResponse)
            )
        )

        whenReady(connector.getContactPreferences(appaId).failed) { e =>
          e.getMessage must include("Invalid JSON format")
        }
      }

      "should fail when an unexpected response is returned" in new SetUp {
        server.stubFor(
          get(urlMatching(getUrl))
            .willReturn(
              aResponse()
                .withStatus(BAD_GATEWAY)
            )
        )

        whenReady(connector.getContactPreferences(appaId).failed) { e =>
          e.getMessage must include("Unexpected response")
        }
      }

      "should fail when an unexpected status code is returned" in new SetUp {
        server.stubFor(
          get(urlMatching(getUrl))
            .willReturn(
              aResponse()
                .withStatus(CREATED)
            )
        )

        whenReady(connector.getContactPreferences(appaId).failed) { e =>
          e.getMessage must include("Unexpected status code: 201")
        }
      }
    }

    "submitReturn" - {
      "should successfully submit a return" in new SetUp {
        val jsonResponse: String =
          Json
            .toJson(contactPreferencesResponse)
            .toString() // TODO Return the appropriate response when we have the API spec

        server.stubFor(
          post(urlMatching(updateUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferencesRequest))))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(jsonResponse)
            )
        )

        whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) {
          case Right(details) =>
            details.paperlessReference shouldBe contactPreferencesResponse.paperlessReference
            details.emailAddress       shouldBe contactPreferencesResponse.emailAddress
            details.emailStatus        shouldBe contactPreferencesResponse.emailStatus
            details.emailBounced       shouldBe contactPreferencesResponse.emailBounced
          case _              => fail("Test failed: result did not match expected value")
        }
      }

      "should fail when invalid JSON is returned" in new SetUp {
        val invalidJsonResponse = """{ "invalid": "json" }"""
        server.stubFor(
          post(urlMatching(updateUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferencesRequest))))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(invalidJsonResponse)
            )
        )

        whenReady(connector.setContactPreferences(appaId, contactPreferencesRequest).value) { result =>
          result.swap.toOption.get must include("Invalid JSON format")
        }
      }

      "fail when update contact preferences returns an error" in new SetUp {
        server.stubFor(
          post(urlMatching(updateUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferencesRequest))))
            .willReturn(
              aResponse()
                .withBody("upstreamErrorResponse")
                .withStatus(BAD_GATEWAY)
            )
        )

        recoverToExceptionIf[Exception] {
          connector.setContactPreferences(appaId, contactPreferencesRequest).value
        } map { ex =>
          ex.getMessage must include("Unexpected response")
        }
      }

      "fail when an unexpected status code is returned" in new SetUp {
        server.stubFor(
          post(urlMatching(updateUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(contactPreferencesRequest))))
            .willReturn(
              aResponse()
                .withBody("invalidStatusCodeResponse")
                .withStatus(BAD_REQUEST)
            )
        )
        recoverToExceptionIf[Exception] {
          connector.setContactPreferences(appaId, contactPreferencesRequest).value
        } map { ex =>
          ex.getMessage must include("Unexpected status code: 201")
        }
      }
    }
  }

  class SetUp {
    val connector: ContactPreferencesConnector = app.injector.instanceOf[ContactPreferencesConnector]
    val getUrl                                 = s"/alcohol-duty-contact-preferences/contactPreferences/$appaId"
    val updateUrl                              = s"/alcohol-duty-contact-preferences/update/contactPreferences/$appaId"
  }
}
