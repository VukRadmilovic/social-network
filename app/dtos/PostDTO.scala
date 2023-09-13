package dtos

import play.api.libs.json.{Json, OFormat}

case class PostDTO (content: String)

object PostDTO {
  implicit val jsonFormat: OFormat[PostDTO] = Json.format[PostDTO]
}
