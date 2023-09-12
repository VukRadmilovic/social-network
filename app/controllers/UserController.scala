package controllers

import actions.JWTAuthAction
import dtos.{LoginAttempt, UserDTO}
import models.User
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import repositories.UserRepository
import services.{FriendRequestService, UserService}

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    val userRepository: UserRepository,
    val userService: UserService,
    val friendRequestService: FriendRequestService,
    val jwtAuthAction: JWTAuthAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {
  def getAll: Action[AnyContent] =
    jwtAuthAction.async {
      userService.getAll.map(users => {
        Ok(Json.toJson(users.map(UserDTO(_))))
      })
    }

  def getByUsername(username: String): Action[AnyContent] =
    jwtAuthAction.async {
      userService.getByUsername(username).map {
        case Some(user) => Ok(Json.toJson(UserDTO(user)))
        case None       => NotFound
      }
    }

  def register(): Action[User] =
    Action.async(parse.json[User]) { implicit request =>
      val user = request.body

      userService.register(user).map(newUser => Created(Json.toJson(UserDTO(newUser))))
  }

  def login(): Action[LoginAttempt] =
    Action.async(parse.json[LoginAttempt]) { implicit request =>
      val loginAttempt = request.body

      userService.login(loginAttempt).map {
        case Some(token) =>
          Ok(Json.obj("token" -> token))
        case None =>
          Unauthorized(Json.obj("message" -> "Your username or password is incorrect"))
      }
  }
}
