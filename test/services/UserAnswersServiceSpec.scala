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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.UserAnswersConnector
import models.ErrorModel
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class UserAnswersServiceSpec extends SpecBase {

  "set" - {
    "should return a success if the operation was successful" in new Setup {
      when(mockUserAnswersConnector.set(any())(any()))
        .thenReturn(Future.successful(HttpResponse(status = OK, body = "success response")))

      whenReady(testService.set(userAnswers).value) {
        _.toString mustBe Right(HttpResponse(status = OK, body = "success response")).toString
      }
    }
    "should return an error if the operation was unsuccessful" in new Setup {
      when(mockUserAnswersConnector.set(any())(any()))
        .thenReturn(Future.successful(HttpResponse(status = INTERNAL_SERVER_ERROR, body = "error response")))

      whenReady(testService.set(userAnswers).value) {
        _ mustBe Left(
          ErrorModel(status = INTERNAL_SERVER_ERROR, message = "Unexpected error setting user answers, status: 500")
        )
      }
    }
  }

  class Setup {
    val mockUserAnswersConnector: UserAnswersConnector = mock[UserAnswersConnector]
    val testService                                    = new UserAnswersService(mockUserAnswersConnector)
  }
}
