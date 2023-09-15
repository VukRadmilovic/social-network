package services

import dtos.OutputPostDTO
import exceptions.{AuthorizationException, NotFoundException, ValidationException}
import models.{Post, User}
import play.api.Logging
import repositories.{PostRepository, UserRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostService @Inject() (
  postRepository: PostRepository,
  userRepository: UserRepository
)(implicit ec: ExecutionContext) extends Logging {
  def like(id: Long, user: String): Future[Unit] = {
    for {
      post <- postRepository.getById(id).map(_.getOrElse(throw NotFoundException("Post with this ID does not exist")))
      areFriends <- userRepository.areFriends(user, post.poster)
      _ <- if (areFriends || post.poster == user) postRepository.like(id, user, post.likes) else
        Future.failed(AuthorizationException("You can only like your friend's posts"))
    } yield ()
  }

  def unlike(id: Long, user: String): Future[Unit] = {
    postRepository.getById(id).flatMap {
      case Some(post) => postRepository.unlike(id, user, post.likes)
      case None => Future.failed(NotFoundException("Post with this ID does not exist"))
    }
  }

  def delete(id: Long, user: String): Future[Unit] = {
    postRepository.getById(id).flatMap {
      case Some(post) if post.poster == user => postRepository.delete(id)
      case Some(_) => Future.failed(AuthorizationException("You can only delete your own posts"))
      case _ => Future.failed(NotFoundException("Post with this ID does not exist"))
    }
  }

  def create(post: Post): Future[OutputPostDTO] = {
    if (post.content.isBlank) {
      Future.failed(ValidationException("Empty post"))
    } else if (post.content.length > 150) {
      Future.failed(ValidationException("Post is too long. Max length in characters is 150."))
    } else {
      postRepository.create(post).map(created => OutputPostDTO(created, liked = false))
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
          Future.failed(NotFoundException("Post with this ID does not exist"))
      }
    }
  }

  def getById(id: Long, user: String): Future[OutputPostDTO] = {
    postRepository.getById(id).flatMap {
      case Some(post) if post.poster == user =>
        postRepository.liked(post.id, user).map(liked => OutputPostDTO(post, liked))
      case Some(post) =>
        userRepository.areFriends(user, post.poster).flatMap {
          case true => postRepository.liked(post.id, user).map(liked => OutputPostDTO(post, liked))
          case false => Future.failed(AuthorizationException("You can only view your friend's posts"))
        }
      case None => Future.failed(NotFoundException("Post with this ID does not exist"))
    }
  }

  private def getTimelineWithLikes(posters: Seq[String], user: String) = {
    for {
      posts <- postRepository.getTimeline(posters)
      likedFutures = posts.map(post => postRepository.liked(post.id, user).map(liked => OutputPostDTO(post, liked)))
      postDTOs <- Future.traverse(likedFutures)(identity)
    } yield postDTOs
  }

  /**
   * Retrieves and returns the timeline of a specified user (friend) for the currently logged-in user (or his own posts).
   *
   * This method retrieves posts posted by a specified user (friend) and returns them as a sequence of Post objects.
   *
   * @param user   The username of the currently logged-in user.
   * @param poster The username of the friend whose timeline is to be retrieved.
   * @return A Future containing a sequence of Post objects representing the posts from the friend's timeline.
   */
  def getFriendTimeline(user: String, poster: String): Future[Seq[OutputPostDTO]] = {
    if (user == poster) {
      getTimelineWithLikes(Seq(poster), user)
    } else {
      userRepository.getByUsername(poster).flatMap {
        case Some(_) => userRepository.areFriends(user, poster).flatMap {
          case true => getTimelineWithLikes(Seq(poster), user)
          case false => Future.failed(AuthorizationException("You can only view your friend's posts"))
        }
        case None => Future.failed(NotFoundException("Poster does not exist"))
      }
    }
  }

  /**
   * Retrieve a user's timeline, including their own posts and those of their friends.
   *
   * This method retrieves a chronological list of posts for a given user, including their own posts
   * and those of their friends. The posts are sorted by creation date, with the latest posts displayed first.
   *
   * @param user The username of the user whose timeline is being retrieved.
   * @return A Future containing a sequence of posts from the user and their friends, sorted by creation date.
   */
  def getTimeline(user: String): Future[Seq[OutputPostDTO]] = {
    for {
      friends <- userRepository.getFriends(user)
      postDTOs <- getTimelineWithLikes(friends :+ user, user)
    } yield postDTOs
  }

  def getLikers(id: Long, user: String): Future[Seq[User]] = {
    postRepository.getById(id).flatMap {
      case Some(post) if post.poster == user => postRepository.getLikers(id)
      case Some(post) =>
        userRepository.areFriends(user, post.poster).flatMap {
          case true => postRepository.getLikers(id)
          case false => Future.failed(AuthorizationException("You can only view information about your friend's posts"))
        }
      case None => Future.failed(NotFoundException("Post with this ID does not exist"))
    }
  }
}
