package models

import dtos.NewUser

case class User(username: String, displayName: String, password: String, email: String, friends: Array[User]) {
  def this(user: NewUser) = {
    this(user.username, user.displayName, user.password, user.email, Array[User]())
  }
}
