package repositories

import models.User

import javax.inject.Inject
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject() (implicit ec: ExecutionContext) {
  private var users = ListBuffer[User]()
  users += User("jova", "Jova", "10000:nPgQghJANf+lmHoINwqgEvl/7whHWlCZHht9xUdVns8=:PdRPv/yXYtn/C0N0xJoikQ==", "jova@mail.com", Array[User]())
  users += User("nika", "Nika", "10000:nPgQghJANf+lmHoINwqgEvl/7whHWlCZHht9xUdVns8=:PdRPv/yXYtn/C0N0xJoikQ==", "nika@mail.com", Array[User]())

  def getAll: Future[ListBuffer[User]] = Future {
    users
  }

  def getByUsername(username: String): Future[Option[User]] = Future {
    users.find(user => user.username == username)
  }

  def getByEmail(email: String): Future[Option[User]] = Future {
    users.find(user => user.email == email)
  }

  def create(user: User): Future[User] = Future {
    users += user
    user
  }
}
