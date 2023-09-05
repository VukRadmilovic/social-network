package repositories

import models.User

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class UserRepository {
  private val users = new ListBuffer[User]()
  users += User("jova", "Jova", "123", "jova@mail.com", Array[User]())
  users += User("nika", "Nika", "456", "nika@mail.com", Array[User]())

  def getByUsername(username: String): Option[User] = {
    users.find(user => user.username == username)
  }

  def getByEmail(email: String): Option[User] = {
    users.find(user => user.email == email)
  }

  def create(user: User): User = {
    users += user
    user
  }

  def getAll: ListBuffer[User] = {
    users
  }
}
