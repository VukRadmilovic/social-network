package repositories

import dtos.PaginatedResult
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

  /**
   * Retrieve a timeline of posts from specified posters, sorted by creation date, in a paginated manner.
   *
   * This method fetches a chronological list of posts from a set of posters, sorting them by their
   * creation date in descending order. It provides a paginated timeline of posts from the specified posters.
   *
   * @param posters The usernames of posters whose posts are included in the timeline.
   * @param limit   The maximum number of posts to retrieve on each page.
   * @param page    The page number for paginating the results (starting from 0).
   * @return JSON representation of a paginated timeline containing posts from the specified posters,
   *         sorted by creation date.
   */
  def getTimeline(posters: Seq[String], limit: Long, page: Long): Future[PaginatedResult[Post]] = db.run {
    val offset = page * limit

    for {
      posts <- postTable.filter(post => post.poster.inSet(posters)).sortBy(_.posted.desc).drop(offset).take(limit).result
      numberOfPosts <- postTable.filter(post => post.poster.inSet(posters)).length.result
    } yield PaginatedResult(
      totalCount = numberOfPosts,
      entries = posts.toList,
      hasNextPage = offset + limit < numberOfPosts
    )
  }

  def getLikers(id: Long, limit: Long, page: Long): Future[PaginatedResult[User]] = db.run {
    val offset = page * limit
    val baseQuery = likesTable.filter(_.post === id)

    for {
      likes <- baseQuery.map(_.username).join(userRepository.userTable).on(_ === _.username)
        .map(usernameUser => usernameUser._2).drop(offset).take(limit).result
      numberOfLikers <- baseQuery.length.result
    } yield PaginatedResult(
      totalCount = numberOfLikers,
      entries = likes.toList,
      hasNextPage = offset + limit < numberOfLikers
    )
  }

  def liked(id: Long, user: String): Future[Boolean] = {
    db.run(likesTable.filter(like => like.post === id && like.username === user).exists.result)
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

  private class PostTable(tag: Tag) extends Table[Post](tag, "posts") {

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

  private class LikesTable(tag: Tag)
    extends Table[(String, Long)](tag, "likes") {
    def username = column[String]("username")
    def post = column[Long]("post")

    def pk = primaryKey("like_fk", (username, post))

    override def * : ProvenShape[(String, Long)] = (username, post)
  }
}
