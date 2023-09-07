package dtos

import models.User
import play.api.libs.json.{Json, OFormat}

case class UserWithFriends(
    username: String,
    displayName: String,
    password: String,
    email: String,
    friends: Array[UserWithFriends]
)

object UserWithFriends {
  implicit val jsonFormat: OFormat[UserWithFriends] =
    Json.format[UserWithFriends]

  def apply(user: User): UserWithFriends = new UserWithFriends(
    user.username,
    user.displayName,
    user.password,
    user.email,
    Array[UserWithFriends]()
  )

  def apply(user: User, hashedPassword: String) = new UserWithFriends(
    user.username,
    user.displayName,
    hashedPassword,
    user.email,
    Array[UserWithFriends]()
  )
}
