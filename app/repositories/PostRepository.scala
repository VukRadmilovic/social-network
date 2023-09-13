package repositories

import exceptions.ValidationException
import models.Post
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostRepository @Inject() (val dbConfigProvider: DatabaseConfigProvider)(
    implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  import profile.api._

  private val postTable = TableQuery[PostTable]

  def create(post: Post): Future[Post] = {
    db.run(postTable returning postTable.map(_.id) += post).map(id => Post.create(post, id)).recover {
      case _: SQLIntegrityConstraintViolationException => throw ValidationException("Poster does not exist")
    }
  }

  class PostTable(tag: Tag) extends Table[Post](tag, "posts") {
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
