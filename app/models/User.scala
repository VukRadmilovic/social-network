package models

import dtos.NewUser

case class User(username: String, displayName: String, password: String, email: String, friends: Array[User]) {
  def this(user: NewUser, hashedPassword: String) = {
    this(user.username, user.displayName, hashedPassword, user.email, Array[User]())
  }
}
