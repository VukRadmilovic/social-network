package repositories

import dtos.UserWithoutFriends
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject() (val dbConfigProvider: DatabaseConfigProvider) (implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val userTable = TableQuery[UserTable]
  private val friendshipTable = TableQuery[FriendshipTable]

  def getAll: Future[Seq[User]] = {
    val query = userTable.result
    val usersWithoutFriendsFuture: Future[Seq[UserWithoutFriends]] = db.run(query)

    usersWithoutFriendsFuture.map(usersWithoutFriends =>
      usersWithoutFriends.map(userWithoutFriends => new User(userWithoutFriends))
    )
  }

  def getByUsername(username: String): Future[Option[User]] = {
    val userWithoutFriends = db.run(userTable.filter(_.username === username).result).map(_.headOption)

    userWithoutFriends.map {
      case Some(user) => Some(new User(user))
      case None => None
    }
  }

  def getByEmail(email: String): Future[Option[User]] = {
    val userWithoutFriends = db.run(userTable.filter(_.email === email).result).map(_.headOption)

    userWithoutFriends.map {
      case Some(user) => Some(new User(user))
      case None => None
    }
  }

  def create(user: User): Future[Option[User]] = {
    val userWithoutFriends = new UserWithoutFriends(user.username, user.displayName, user.password, user.email)

    db.run(userTable += userWithoutFriends)
      .map(_ => Some(user))
      .recover {
        case e: Throwable =>
          e.printStackTrace()
          None
      }
  }

  class UserTable(tag: Tag) extends Table[UserWithoutFriends](tag, "users") {
    def username = column[String]("username", O.PrimaryKey)
    def displayName = column[String]("displayName")
    def password = column[String]("password")
    def email = column[String]("email", O.Unique)
    override def * : ProvenShape[UserWithoutFriends] = (username, displayName, password, email) <> ((UserWithoutFriends.apply _).tupled, UserWithoutFriends.unapply)
  }

  class FriendshipTable(tag: Tag) extends Table[(String, String)](tag, "friendships") {
    def username1 = column[String]("username1")
    def username2 = column[String]("username2")

    def user1 = foreignKey("user1_fk", username1, friendshipTable)(_.username1)
    def user2 = foreignKey("user2_fk", username2, friendshipTable)(_.username2)

    override def * : ProvenShape[(String, String)] = (username1, username2)
  }
}
