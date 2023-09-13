package services

import exceptions.{AuthorizationException, ValidationException}
import models.Post
import play.api.Logging
import repositories.PostRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostService @Inject() (
  postRepository: PostRepository
)(implicit ec: ExecutionContext) extends Logging {
  def create(post: Post): Future[Post] = {
    if (post.content.isBlank) {
      Future.failed(ValidationException("Empty post"))
    } else {
      postRepository.create(post)
    }
  }

  def edit(id: Long, username: String, newContent: String): Future[Unit] = {
    if (newContent.isBlank) {
      Future.failed(ValidationException("Empty edited post"))
    } else {
      postRepository.getById(id).flatMap {
        case Some(post) if post.poster == username =>
          postRepository.edit(id, newContent)
        case Some(_) =>
          Future.failed(AuthorizationException("You can only edit your own posts"))
        case None =>
          Future.failed(ValidationException("Post with this ID does not exist"))
      }
    }
  }
}
