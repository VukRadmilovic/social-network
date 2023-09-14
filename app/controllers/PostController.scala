package controllers

import actions.JWTAuthAction
import dtos.{PostDTO, UserDTO}
import helpers.RequestKeys.TokenUsername
import models.Post
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.PostService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PostController @Inject() (
    val controllerComponents: ControllerComponents,
    val postService: PostService,
    val jwtAuthAction: JWTAuthAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {
  def create(): Action[PostDTO] =
    jwtAuthAction.async(parse.json[PostDTO]) { implicit request =>
      val postDTO = request.body
      val username = request.attrs.get(TokenUsername).get

      postService.create(Post.create(postDTO, username)).map(post => Created(Json.toJson(post)))
    }

  def edit(id: Long): Action[PostDTO] =
    jwtAuthAction.async(parse.json[PostDTO]) { implicit request =>
      val postDTO = request.body
      val username = request.attrs.get(TokenUsername).get

      postService.edit(id, username, postDTO.content).map(_ => NoContent)
    }

  def like(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.like(id, username).map(_ => NoContent)
    }

  def unlike(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.unlike(id, username).map(_ => NoContent)
    }

  def delete(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.delete(id, username).map(_ => NoContent)
    }

  def getById(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.getById(id, username).map(post => Ok(Json.toJson(post)))
    }

  /**
   * Retrieves and returns the timeline of a friend of the currently logged-in user (or his own posts).
   *
   * This method retrieves posts posted by a specified user (friend) and returns them as a JSON response.
   *
   * @param poster The username of the friend whose timeline is to be retrieved.
   * @return A JSON response containing the posts from the friend's timeline.
   */
  def getFriendTimeline(poster: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.getFriendTimeline(username, poster).map(posts => Ok(Json.toJson(posts)))
    }

  def getLikers(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.getLikers(id, username).map(likers => Ok(Json.toJson(likers.map(UserDTO(_)))))
    }

  /**
   * Retrieve and return a user's timeline, including their own posts and those of their friends.
   *
   * This endpoint provides a chronological list of posts, with the latest posts displayed first.
   *
   * @return A JSON array containing posts from the user and their friends, sorted by creation date.
   */
  def getTimeline: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      postService.getTimeline(username).map(posts => Ok(Json.toJson(posts)))
    }
}
