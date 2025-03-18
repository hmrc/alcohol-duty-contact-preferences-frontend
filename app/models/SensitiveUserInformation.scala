package models

import play.api.libs.json.{Json, OFormat}

case class SensitiveUserInformation(
                                   emailAddress: Option[String],
                                   emailEntered: Option[String] = None
                                   )

object SensitiveUserInformation {
  implicit val format: OFormat[SensitiveUserInformation] = Json.format[SensitiveUserInformation]
}
