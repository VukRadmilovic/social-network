package controllers

import actions.JWTAuthAction
import dtos.LoginAttempt
import exceptions.AlreadyFriendsException
import helpers.RequestKeys.TokenUsername
import models.User
import play.api.libs.json._
import play.api.mvc._
import repositories.UserRepository
import services.UserService

import javax.inject._
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

      userService
        .register(user)
        .map(newUser => Created(Json.toJson(newUser)))
        .recover(e => BadRequest(Json.obj("message" -> e.getMessage)))
  }

  def login(): Action[LoginAttempt] = Action.async(parse.json[LoginAttempt]) {
    implicit request =>
      val loginAttempt = request.body

      userService.login(loginAttempt).map {
        case Some(token) =>
          Ok(Json.obj("token" -> token))
        case None =>
          BadRequest(
            Json.obj("message" -> "Your username or password is incorrect")
          )
      }
  }

  def addFriends(username1: String, username2: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val requesterUsername = request.attrs.get(TokenUsername).getOrElse("")
      if (requesterUsername != username1) {
        Future.successful(Forbidden)
      } else {
        userService
          .addFriends(username1, username2)
          .map(_ => NoContent)
          .recover(e => BadRequest(Json.obj("message" -> e.getMessage)))
      }
    }
}
