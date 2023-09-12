package dtos

import play.api.libs.json.{Json, OFormat}

case class EmailChangeDTO (currentPassword: String, newEmail: String)

object EmailChangeDTO {
  implicit val jsonFormat: OFormat[EmailChangeDTO] = Json.format[EmailChangeDTO]
}
