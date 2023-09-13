package controllers

import actions.JWTAuthAction
import dtos.PostDTO
import helpers.RequestKeys.TokenUsername
import models.Post
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, BaseController, ControllerComponents}
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
}
