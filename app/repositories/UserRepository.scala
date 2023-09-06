package repositories

import dtos.UserWithFriends
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

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
    val usersWithoutFriendsFuture: Future[Seq[User]] = db.run(query)

    usersWithoutFriendsFuture.map(usersWithoutFriends =>
      usersWithoutFriends.map(userWithoutFriends =>
        new UserWithFriends(userWithoutFriends)
      )
    )
  }

  def getByUsername(username: String): Future[Option[UserWithFriends]] = {
    val userWithoutFriends =
      db.run(userTable.filter(_.username === username).result).map(_.headOption)

    userWithoutFriends.map {
      case Some(user) => Some(new UserWithFriends(user))
      case None       => None
    }
  }

  def getByEmail(email: String): Future[Option[UserWithFriends]] = {
    val userWithoutFriends =
      db.run(userTable.filter(_.email === email).result).map(_.headOption)

    userWithoutFriends.map {
      case Some(user) => Some(new UserWithFriends(user))
      case None       => None
    }
  }

  def create(user: UserWithFriends): Future[UserWithFriends] = {
    val userWithoutFriends =
      new User(user.username, user.displayName, user.password, user.email)

    db.run(userTable += userWithoutFriends)
      .map(_ => user)
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

    def user1 = foreignKey("user1_fk", username1, friendshipTable)(_.username1)
    def user2 = foreignKey("user2_fk", username2, friendshipTable)(_.username2)

    override def * : ProvenShape[(String, String)] = (username1, username2)
  }
}
