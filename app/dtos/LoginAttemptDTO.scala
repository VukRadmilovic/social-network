package dtos

import play.api.libs.json.{Json, OFormat}

case class LoginAttemptDTO(username: String, password: String)

object LoginAttemptDTO {
  implicit val jsonFormat: OFormat[LoginAttemptDTO] = Json.format[LoginAttemptDTO]
}
