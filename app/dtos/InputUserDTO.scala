package dtos

import play.api.libs.json.{Json, OFormat}

case class InputUserDTO(
                 username: String,
                 displayName: String,
                 password: String,
                 email: String
               )

object InputUserDTO {
  implicit val jsonFormat: OFormat[InputUserDTO] = Json.format[InputUserDTO]
}