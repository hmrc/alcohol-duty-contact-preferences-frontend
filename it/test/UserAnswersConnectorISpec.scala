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
import connectors.UserAnswersConnector
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json

class UserAnswersConnectorISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = applicationBuilder(None)
    .configure("microservice.services.alcohol-duty-contact-preferences.port" -> server.port())
    .build()

  "UserAnswersConnector" - {

    "get must" - {
      "successfully fetch user answers" in new SetUp {
        val jsonResponse = Json.toJson(emptyUserAnswers).toString()
        server.stubFor(
          get(urlMatching(userAnswersGetUrl))
            .willReturn(aResponse().withStatus(OK).withBody(jsonResponse))
        )

        whenReady(connector.get(appaId)) { result =>
          result mustBe Right(emptyUserAnswers)
        }
      }

      "return an error when the upstream service returns an error" in new SetUp {
        server.stubFor(
          get(urlMatching(userAnswersGetUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        whenReady(connector.get(appaId)) { result =>
          result.isLeft mustBe true
          result.swap.toOption.get.statusCode mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "createUserAnswers must" - {
      "successfully write user answers" in new SetUp {
        server.stubFor(
          post(urlMatching(userAnswersUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(userDetails))))
            .willReturn(
              aResponse()
                .withStatus(CREATED)
                .withBody(Json.stringify(Json.toJson(emptyUserAnswers)))
            )
        )

        whenReady(connector.createUserAnswers(userDetails)) {
          case Right(userAnswersResponse) =>
            userAnswersResponse mustBe emptyUserAnswers
          case Left(_)                    =>
            fail("Expected Right(UserAnswers), but got Left(UpstreamErrorResponse)")
        }
      }

      "fail to write user answers when the service returns an error" in new SetUp {
        server.stubFor(
          post(urlMatching(userAnswersUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(userDetails))))
            .willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        whenReady(connector.createUserAnswers(userDetails)) { result =>
          result.isLeft mustBe true
          result.swap.toOption.get.statusCode mustBe BAD_REQUEST
        }
      }
    }

    "set must" - {
      "successfully write user answers" in new SetUp {
        server.stubFor(
          put(urlMatching(userAnswersUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(emptyUserAnswers))))
            .willReturn(aResponse().withStatus(OK))
        )

        whenReady(connector.set(emptyUserAnswers)) { result =>
          result.status mustBe OK
        }
      }

      "fail to write user answers when the service returns an error" in new SetUp {
        server.stubFor(
          put(urlMatching(userAnswersUrl))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(emptyUserAnswers))))
            .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
        )

        whenReady(connector.set(emptyUserAnswers)) { result =>
          result.status mustBe SERVICE_UNAVAILABLE
        }
      }
    }

    "releaseLock" - {
      "must call the release lock endpoint" in new SetUp{
        server.stubFor(
          delete(urlMatching(releaseLockUrl))
            .willReturn(aResponse().withStatus(OK))
        )

        whenReady(connector.releaseLock(appaId)) {
          result =>
            result.status mustBe OK
        }
      }

      "return an error when the upstream service returns an error" in new SetUp {
        server.stubFor(
          delete(urlMatching(releaseLockUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        whenReady(connector.releaseLock(appaId)) { result =>
          result.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "keepAlive" - {
      "must call the keep alive endpoint" in new SetUp {
        server.stubFor(
          put(urlMatching(keepAliveUrl))
            .willReturn(aResponse().withStatus(OK))
        )

        whenReady(connector.keepAlive(appaId)) { response =>
          response.status mustBe OK
        }
      }

      "return an error when the upstream service returns an error" in new SetUp {
        server.stubFor(
          put(urlMatching(keepAliveUrl))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )
        whenReady(connector.keepAlive(appaId)) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  class SetUp {
    val connector         = app.injector.instanceOf[UserAnswersConnector]
    val userAnswersGetUrl = s"/alcohol-duty-contact-preferences/user-answers/$appaId"
    val userAnswersUrl    = "/alcohol-duty-contact-preferences/user-answers"
    val releaseLockUrl    = s"/alcohol-duty-contact-preferences/user-answers/lock/$appaId"
    val keepAliveUrl      = s"/alcohol-duty-contact-preferences/user-answers/lock/$appaId/ttl"
  }
}
