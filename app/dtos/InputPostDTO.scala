package dtos

import play.api.libs.json.{Json, OFormat}

case class InputPostDTO(content: String)

object InputPostDTO {
  implicit val jsonFormat: OFormat[InputPostDTO] = Json.format[InputPostDTO]
}
