package repositories

import models.User
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject() (val dbConfigProvider: DatabaseConfigProvider)(
    implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  import profile.api._

  val userTable = TableQuery[UserTable]
  private val friendshipTable = TableQuery[FriendshipTable]

  def getAll: Future[Seq[User]] = {
    db.run(userTable.result)
  }

  def getByUsernameOrDisplayNameStartsWith(name: String): Future[Seq[User]] = {
    db.run(userTable.filter(user =>
      user.username.toLowerCase.startsWith(name) || user.displayName.toLowerCase.startsWith(name)).result)
  }

  def getByUsername(username: String): Future[Option[User]] = {
    db.run(userTable.filter(_.username === username).result).map(_.headOption)
  }

  def getByEmail(email: String): Future[Option[User]] = {
    db.run(userTable.filter(_.email === email).result).map(_.headOption)
  }

  def create(user: User): Future[User] = {
    db.run(userTable += user).map(_ => user)
  }

  def addFriends(username1: String, username2: String): Future[Unit] = {
    db.run(friendshipTable += (username1, username2)).map(_ => ())
  }

  def areFriends(username1: String, username2: String): Future[Boolean] = {
    db.run(
        friendshipTable
        .filter(friendship =>
          (friendship.username1 === username1 && friendship.username2 === username2) ||
          (friendship.username2 === username1 && friendship.username1 === username2))
        .exists
        .result)
  }

  def getFriends(username: String): Future[Seq[String]] = {
    db.run(
      friendshipTable
        .filter(friendship => friendship.username1 === username || friendship.username2 === username)
        .result
    ).map(friendTuples => friendTuples.flatMap { case (user1, user2) =>
      Seq(user1, user2).filterNot(_ == username)
    })
  }

  def changeDisplayName(username: String, newDisplayName: String): Future[Unit] = {
    db.run(userTable.filter(_.username === username).map(_.displayName).update(newDisplayName)).map(_ => ())
  }

  def changePassword(username: String, newPassword: String): Future[Unit] = {
    db.run(userTable.filter(_.username === username).map(_.password).update(newPassword)).map(_ => ())
  }

  def changeEmail(username: String, newEmail: String): Future[Unit] = {
    db.run(userTable.filter(_.username === username).map(_.email).update(newEmail)).map(_ => ())
  }

  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def username = column[String]("username", O.PrimaryKey)
    def displayName = column[String]("displayName")
    def password = column[String]("password")
    def email = column[String]("email", O.Unique)
    override def * : ProvenShape[User] = (
      username,
      displayName,
      password,
      email
    ) <> ((User.apply _).tupled, User.unapply)
  }

  private class FriendshipTable(tag: Tag)
      extends Table[(String, String)](tag, "friendships") {
    def username1 = column[String]("username1")
    def username2 = column[String]("username2")

    def pk = primaryKey("friendship", (username1, username2))

    def user1 = foreignKey("user1_fk", username1, userTable)(_.username)
    def user2 = foreignKey("user2_fk", username2, userTable)(_.username)

    override def * : ProvenShape[(String, String)] = (username1, username2)
  }
}
