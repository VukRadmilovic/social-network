package dtos

import models.User
import play.api.libs.json.{Json, OFormat}

case class UserDTO (username: String, displayName: String, email: String)

object UserDTO {
  implicit val jsonFormat: OFormat[UserDTO] = Json.format[UserDTO]

  def apply(user: User): UserDTO = new UserDTO(user.username, user.displayName, user.email)
}