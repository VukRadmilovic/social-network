package dtos

import models.User
import play.api.libs.json.{Json, OFormat}

case class OutputUserDTO(username: String, displayName: String, email: String)

object OutputUserDTO {
  implicit val jsonFormat: OFormat[OutputUserDTO] = Json.format[OutputUserDTO]

  def apply(user: User): OutputUserDTO = new OutputUserDTO(user.username, user.displayName, user.email)
}