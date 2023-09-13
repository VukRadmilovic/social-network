package services

import exceptions.ValidationException
import models.Post
import repositories.PostRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostService @Inject() (
  postRepository: PostRepository
)(implicit ec: ExecutionContext) {
  def create(post: Post): Future[Post] = {
    if (post.content.isBlank) {
      Future.failed(ValidationException("Empty post"))
    } else {
      postRepository.create(post)
    }
  }
}
