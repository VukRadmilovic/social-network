package controllers

import actions.JWTAuthAction
import dtos.{LoginAttempt, UserWithFriends}
import helpers.RequestKeys.TokenUsername
import models.User

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import repositories.UserRepository
import services.UserService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    val userRepository: UserRepository,
    val userService: UserService,
    val jwtAuthAction: JWTAuthAction
)(implicit ec: ExecutionContext)
    extends BaseController {
  def getAll: Action[AnyContent] = jwtAuthAction.async {
    userService.getAll.map(users => Ok(Json.toJson(users)))
  }

  def getByUsername(username: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      println(request.attrs.get(TokenUsername).getOrElse(""))
      userService.getByUsername(username).map {
        case Some(user) => Ok(Json.toJson(user))
        case None       => NotFound
      }
    }

  def register(): Action[User] = Action.async(parse.json[User]) {
    implicit request =>
      val user = request.body

      userService.register(user).map {
        case Right(newUser) =>
          Created(Json.toJson(newUser))
        case Left(result) =>
          result
      }
  }

  def login(): Action[LoginAttempt] = Action.async(parse.json[LoginAttempt]) {
    implicit request =>
      val loginAttempt = request.body

      userService.login(loginAttempt).map {
        case Some(token) =>
          Ok(Json.obj("token" -> token))
        case None =>
          BadRequest("Your username or password is incorrect")
      }
  }
}
