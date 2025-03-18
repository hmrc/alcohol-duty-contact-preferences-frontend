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

package models

import play.api.libs.json._
import queries.{Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
                              appaId: String,
                              userId: String,
                              paperlessReference: Boolean,
                              emailVerification: Option[Boolean],
                              bouncedEmail: Option[Boolean],
                              //                              sensitiveUserInformation: SensitiveUserInformation,
                              emailAddress: Option[String],
                              emailEntered: Option[String] = None,
                              data: JsObject = Json.obj(),
                              startedTime: Instant,
                              lastUpdated: Instant
                            ) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(Some(value), updatedAnswers)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(None, updatedAnswers)
    }
  }
}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "appaId").read[String] and
        (__ \ "userId").read[String] and
        (__ \ "paperlessReference").read[Boolean] and
        (__ \ "emailVerification").readNullable[Boolean] and
        (__ \ "bouncedEmail").readNullable[Boolean] and
        //        (__ \ "sensitiveUserInformation").read[SensitiveUserInformation] and
        (__ \ "emailAddress").readNullable[String] and
        (__ \ "emailEntered").readNullable[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "startedTime").read(MongoJavatimeFormats.instantFormat) and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "appaId").write[String] and
        (__ \ "userId").write[String] and
        (__ \ "paperlessReference").write[Boolean] and
        (__ \ "emailVerification").writeNullable[Boolean] and
        (__ \ "bouncedEmail").writeNullable[Boolean] and
        //        (__ \ "sensitiveUserInformation").write[SensitiveUserInformation] and
        (__ \ "emailAddress").writeNullable[String] and
        (__ \ "emailEntered").writeNullable[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "startedTime").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(UserAnswers.unapply))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
