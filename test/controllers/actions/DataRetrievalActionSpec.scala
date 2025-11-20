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

import base.SpecBase
import connectors.UserAnswersConnector
import models.requests.{IdentifierRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UpstreamErrorResponse


import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.when

import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase {

  class Harness(userAnswersConnector: UserAnswersConnector) extends DataRetrievalActionImpl(userAnswersConnector) {
    def actionRefine[A](request: IdentifierRequest[A]): Future[Either[Result, OptionalDataRequest[A]]] = refine(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {
        val mockUpstreamErrorResponse = mock[UpstreamErrorResponse]
        when(mockUpstreamErrorResponse.statusCode).thenReturn(NOT_FOUND)

        val userAnswersConnector = mock[UserAnswersConnector]
        when(userAnswersConnector.get(eqTo(appaId))(any())) thenReturn Future(
          Left(mockUpstreamErrorResponse)
        )
        val action               = new Harness(userAnswersConnector)

        val result = action.actionRefine(IdentifierRequest(FakeRequest(), appaId, groupId, userId, credId)).futureValue

        result.isRight mustBe true
        result.map { dataRetrievalRequest =>
          dataRetrievalRequest.userAnswers must not be defined
        }
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {
        val userAnswersConnector = mock[UserAnswersConnector]
        when(userAnswersConnector.get(eqTo(appaId))(any)) thenReturn Future(Right(userAnswers))
        val action               = new Harness(userAnswersConnector)

        val result =
          action
            .actionRefine(IdentifierRequest(FakeRequest(), appaId, groupId, userId, credId))
            .futureValue

        result.isRight mustBe true
        result.map { dataRetrievalRequest =>
          dataRetrievalRequest.userAnswers mustBe defined
        }
      }
    }

    "when the UserAnswersConnector returns an error" - {

      "must redirect to the Journey Recovery controller" in {

        val mockUpstreamErrorResponse = mock[UpstreamErrorResponse]
        when(mockUpstreamErrorResponse.statusCode).thenReturn(BAD_REQUEST)

        val userAnswersConnector = mock[UserAnswersConnector]
        when(userAnswersConnector.get(eqTo(appaId))(any())) thenReturn Future(
          Left(mockUpstreamErrorResponse)
        )
        val action               = new Harness(userAnswersConnector)

        val result = action.actionRefine(IdentifierRequest(FakeRequest(), appaId, groupId, userId, credId))

        val redirectResult = result.map {
          case Left(res) => res
          case _         => fail()
        }
        status(redirectResult) mustBe SEE_OTHER
        redirectLocation(redirectResult).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
