package models

import dtos.UserWithoutFriends
import play.api.libs.json.{Json, OFormat}

case class User(username: String, displayName: String, password: String, email: String, friends: Array[User]) {
  def this(user: UserWithoutFriends, hashedPassword: String) = {
    this(user.username, user.displayName, hashedPassword, user.email, Array[User]())
  }
  def this(user: UserWithoutFriends) = {
    this(user.username, user.displayName, user.password, user.email, Array[User]())
  }
}

object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]
}
