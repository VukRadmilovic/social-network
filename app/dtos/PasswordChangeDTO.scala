package dtos

import play.api.libs.json.{Json, OFormat}

case class PasswordChangeDTO (oldPassword: String, newPassword: String)

object PasswordChangeDTO {
  implicit val jsonFormat: OFormat[PasswordChangeDTO] = Json.format[PasswordChangeDTO]
}