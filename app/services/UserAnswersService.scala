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

import cats.data.EitherT
import connectors.UserAnswersConnector
import models.{ErrorModel, UserAnswers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAnswersService @Inject() (userAnswersConnector: UserAnswersConnector)(implicit ec: ExecutionContext) {

  def set(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): EitherT[Future, ErrorModel, HttpResponse] = EitherT {
    userAnswersConnector.set(userAnswers).map { response =>
      if (response.status >= 200 && response.status < 300) {
        Right(response)
      } else {
        Left(ErrorModel(response.status, s"Unexpected error setting user answers, status: ${response.status})"))
      }
    }
  }

}
