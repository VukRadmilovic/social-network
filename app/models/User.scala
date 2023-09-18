package models

import dtos.InputUserDTO
import play.api.libs.json.{Json, OFormat}

case class User(
    username: String,
    displayName: String,
    password: String,
    email: String,
    hasProfilePicture: Boolean
)

object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]

  def create(user: InputUserDTO) = new User(user.username, user.displayName, user.password, user.email, false)
}