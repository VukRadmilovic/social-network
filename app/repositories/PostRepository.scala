package repositories

import exceptions.ValidationException
import models.{Post, User}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostRepository @Inject() (
                                 val dbConfigProvider: DatabaseConfigProvider,
                                 val userRepository: UserRepository
                               )(
    implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  import profile.api._

  private val postTable = TableQuery[PostTable]
  private val likesTable = TableQuery[LikesTable]

  def like(id: Long, user: String, currentLikes: Long): Future[Unit] = {
    db.run(likesTable += (user, id)).flatMap { _ =>
      db.run(postTable.filter(_.id === id).map(_.likes).update(currentLikes + 1)).map(_ => ())
    }.recover {
      case _: SQLIntegrityConstraintViolationException =>
        throw ValidationException("You already liked this post")
    }
  }

  def unlike(id: Long, user: String, currentLikes: Long): Future[Unit] = {
    db.run(likesTable.filter(likedPost => likedPost.post === id && likedPost.username === user).delete).flatMap {
      case 1 => db.run(postTable.filter(_.id === id).map(_.likes).update(currentLikes - 1)).map(_ => ())
      case 0 => Future.failed(ValidationException("You can only unlike a post you liked"))
    }
  }

  def getById(id: Long): Future[Option[Post]] = {
    db.run(postTable.filter(_.id === id).result).map(_.headOption)
  }

  def getNewestByPoster(poster: String): Future[Seq[Post]] = {
    db.run(postTable.filter(_.poster === poster).sortBy(_.posted.desc).result)
  }

  def getAllLikers(id: Long): Future[Seq[User]] = {
    db.run(likesTable
      .filter(_.post === id)
      .map(_.username)
      .join(userRepository.userTable)
      .on(_ === _.username)
      .map(usernameUser => usernameUser._2)
      .result
    )
  }

  def create(post: Post): Future[Post] = {
    db.run(postTable returning postTable.map(_.id) += post).map(id => Post.create(post, id))
  }

  def edit(id: Long, newContent: String): Future[Unit] = {
    db.run(postTable.filter(_.id === id).map(_.content).update(newContent)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = {
    db.run(postTable.filter(_.id === id).delete).map(_ => ())
  }

  class PostTable(tag: Tag) extends Table[Post](tag, "posts") {

    implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
      ldt => ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
      str => LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    )

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def poster = column[String]("poster")
    def content = column[String]("content")
    def posted = column[LocalDateTime]("posted")
    def likes = column[Long]("likes", O.Default(0))

    override def * : ProvenShape[Post] = (
      id,
      poster,
      content,
      posted,
      likes
    ) <> ((Post.apply _).tupled, Post.unapply)
  }

  class LikesTable(tag: Tag)
    extends Table[(String, Long)](tag, "likes") {
    def username = column[String]("username")
    def post = column[Long]("post")

    def pk = primaryKey("like_fk", (username, post))

    override def * : ProvenShape[(String, Long)] = (username, post)
  }
}
