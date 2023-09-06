package dtos

import play.api.libs.json.{Json, OFormat}

case class UserWithoutFriends(username: String, displayName: String, password: String, email: String)

object UserWithoutFriends {
  implicit val jsonFormat: OFormat[UserWithoutFriends] = Json.format[UserWithoutFriends]
}