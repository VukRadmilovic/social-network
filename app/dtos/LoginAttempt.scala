package dtos

import play.api.libs.json.{Json, OFormat}

case class LoginAttempt (username: String, password: String)

object LoginAttempt {
  implicit val jsonFormat: OFormat[LoginAttempt] = Json.format[LoginAttempt]
}
