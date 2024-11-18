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
import play.api.Application
import play.api.http.Status.{BAD_GATEWAY, CREATED, OK}
import play.api.libs.json.Json

class ContactPreferencesConnectorISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = applicationBuilder(None)
    .configure("microservice.services.alcohol-duty-contact-preferences.port" -> server.port())
    .build()

  "ContactPreferencesConnector" - {
    "getContactPreferences" - {
      "should successfully retrieve contact preferences" in new SetUp {
        val contactPreferencesResponse = new ContactPreferences("1", None, None, None)
        val jsonResponse: String       = Json.toJson(contactPreferencesResponse).toString()

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
        server.stubFor(get(urlMatching(getUrl))
          .willReturn(aResponse()
            .withStatus(OK)
            .withBody(invalidJsonResponse)))

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
  }

  class SetUp {
    val connector: ContactPreferencesConnector = app.injector.instanceOf[ContactPreferencesConnector]
    val getUrl                                 = s"/alcohol-duty-contact-preferences/contactPreferences/$idType/$appaId/$regime"
    val updateUrl                              = s"/alcohol-duty-contact-preferences/update/contactPreferences/$idType/$appaId/$regime"
  }
}
