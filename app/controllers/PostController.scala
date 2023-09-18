package controllers

import actions.JWTAuthAction
import dtos.{InputPostDTO, OutputPostDTO, PaginatedResult, UserDTO}
import helpers.RequestKeys.TokenUsername
import models.Post
import play.api.{Configuration, Logging}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.PostService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

//noinspection ScalaDocUnknownParameter
@Singleton
class PostController @Inject() (
    val controllerComponents: ControllerComponents,
    val postService: PostService,
    val jwtAuthAction: JWTAuthAction,
    val configuration: Configuration
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val paginatedResultPostJsonFormat: OFormat[PaginatedResult[OutputPostDTO]] = Json.format[PaginatedResult[OutputPostDTO]]
  implicit val paginatedResultUserJsonFormat: OFormat[PaginatedResult[UserDTO]] = Json.format[PaginatedResult[UserDTO]]

  def create(): Action[InputPostDTO] =
    jwtAuthAction.async(parse.json[InputPostDTO]) { implicit request =>
      val postDTO = request.body
      val username = request.attrs.get(TokenUsername).get

      postService.create(Post.create(postDTO, username)).map(post => Created(Json.toJson(post)))
    }

  def edit(id: Long): Action[InputPostDTO] =
    jwtAuthAction.async(parse.json[InputPostDTO]) { implicit request =>
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
   * Retrieves and returns a paginated timeline of a friend of the currently logged-in user (or his own posts).
   *
   * This method retrieves posts posted by a specified user (friend) and returns them as a JSON response.
   *
   * @param poster The username of the friend whose timeline is to be retrieved.
   * @param limit  The maximum number of posts to retrieve in each page.
   * @param page   The page number for paginating the results (starting from 0).
   * @return A JSON response containing the posts from the friend's timeline.
   */
  def getFriendTimeline(poster: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(configuration.get[Long]("entriesPerPage"))
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      postService.getFriendTimeline(username, poster, limit, page).map(posts => Ok(Json.toJson(posts)))
    }

  def getLikers(id: Long): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(configuration.get[Long]("entriesPerPage"))
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      postService.getLikers(id, username, limit, page)
        .map(likers => Ok(Json.toJson(PaginatedResult(
          likers.totalCount, likers.entries.map(UserDTO(_)), likers.hasNextPage))))
    }

  /**
   * Retrieve and return a user's paginated timeline, including their own posts and those of their friends.
   *
   * This endpoint provides a chronological list of posts, with the latest posts displayed first.
   *
   * @param limit The maximum number of posts to retrieve in each page.
   * @param page  The page number for paginating the results (starting from 0).
   * @return A JSON array containing posts from the user and their friends, sorted by creation date.
   */
  def getTimeline: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(configuration.get[Long]("entriesPerPage"))
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      postService.getTimeline(username, limit, page).map(posts => Ok(Json.toJson(posts)))
    }
}
