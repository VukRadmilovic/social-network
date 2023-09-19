package models

import dtos.InputUserDTO
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.GetResult

case class User(
    username: String,
    displayName: String,
    password: String,
    email: String,
    hasProfilePicture: Boolean
)

object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]
  implicit val getResult: GetResult[User] = GetResult(r => User(r.nextString(), r.nextString(), r.nextString(), r.nextString(), r.nextBoolean()))

  def create(user: InputUserDTO) = new User(user.username, user.displayName, user.password, user.email, false)
}