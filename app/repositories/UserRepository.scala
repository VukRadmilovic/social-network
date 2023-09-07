package repositories

import dtos.UserWithFriends
import exceptions.AlreadyFriendsException
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.sql.SqlProfile.ColumnOption.SqlType

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject() (val dbConfigProvider: DatabaseConfigProvider)(
    implicit ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val userTable = TableQuery[UserTable]
  private val friendshipTable = TableQuery[FriendshipTable]

  def getAll: Future[Seq[UserWithFriends]] = {
    val query = userTable.result
    val userFuture: Future[Seq[User]] = db.run(query)

    userFuture.map(users => users.map(user => UserWithFriends(user)))
  }

  def getByUsername(username: String): Future[Option[UserWithFriends]] = {
    val user =
      db.run(userTable.filter(_.username === username).result).map(_.headOption)

    user.map {
      case Some(userWithFriends) => Some(UserWithFriends(userWithFriends))
      case None                  => None
    }
  }

  def getByEmail(email: String): Future[Option[UserWithFriends]] = {
    val user =
      db.run(userTable.filter(_.email === email).result).map(_.headOption)

    user.map {
      case Some(userWithFriends) => Some(UserWithFriends(userWithFriends))
      case None                  => None
    }
  }

  def create(userWithFriends: UserWithFriends): Future[UserWithFriends] = {
    val user =
      new User(
        userWithFriends.username,
        userWithFriends.displayName,
        userWithFriends.password,
        userWithFriends.email
      )

    db.run(userTable += user)
      .map(_ => userWithFriends)
  }

  def addFriends(username1: String, username2: String): Future[Unit] = {
    db.run(friendshipTable += (username1, username2))
      .flatMap(success => {
        if (success > 0) {
          Future.successful(())
        } else {
          Future.failed(AlreadyFriendsException())
        }
      })
      .recover(_ => {
        throw AlreadyFriendsException()
      })
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

  class FriendshipTable(tag: Tag)
      extends Table[(String, String)](tag, "friendships") {
    def username1 = column[String]("username1")
    def username2 = column[String]("username2")

    def pk = primaryKey("friendship", (username1, username2))

    def user1 = foreignKey("user1_fk", username1, userTable)(_.username)
    def user2 = foreignKey("user2_fk", username2, userTable)(_.username)

    override def * : ProvenShape[(String, String)] = (username1, username2)
  }
}
