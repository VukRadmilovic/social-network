package dtos

import play.api.libs.json.{Json, OFormat}

case class RefreshTokenDTO (refreshToken: String)

object RefreshTokenDTO {
  implicit val jsonFormat: OFormat[RefreshTokenDTO] = Json.format[RefreshTokenDTO]
}
