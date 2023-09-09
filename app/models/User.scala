package models

import play.api.libs.json.{Json, OFormat}

case class User(
    username: String,
    displayName: String,
    password: String,
    email: String
)

object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]
}