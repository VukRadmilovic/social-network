package controllers

import dtos.{LoginAttempt, NewUser}
import models.User

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import repositories.UserRepository
import services.UserService

import scala.concurrent.{ExecutionContext, Future}


// TODO: use database
@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents,
                               val userRepository: UserRepository,
                               val userService: UserService)
                              (implicit ec: ExecutionContext) extends BaseController {
  implicit val usersJson: OFormat[User] = Json.format[User]
  implicit val newUsersJson: OFormat[NewUser] = Json.format[NewUser]
  implicit val loginAttemptJson: OFormat[LoginAttempt] = Json.format[LoginAttempt]

  def getAll: Action[AnyContent] = Action.async {
    userService.getAll.map(users => Ok(Json.toJson(users)))
  }

  def getByUsername(username: String): Action[AnyContent] = Action.async {
    userService.getByUsername(username).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound(s"User with username: $username doesn't exist")
    }
  }

  def register(): Action[AnyContent] = Action.async { implicit request =>
    val body = request.body.asJson
    val user: Option[NewUser] =
      body.flatMap(
        Json.fromJson[NewUser](_).asOpt
      )

    user match {
      case Some(user) =>
        userService.register(user).map {
          case Right(newUser) =>
            Created(Json.toJson(newUser))
          case Left(result) =>
            result
        }
      case None =>
        Future.successful(BadRequest)
    }
  }

  def login(): Action[AnyContent] = Action.async { implicit request =>
    val body = request.body.asJson
    val attempt: Option[LoginAttempt] =
      body.flatMap(
        Json.fromJson[LoginAttempt](_).asOpt
      )

    attempt match {
      case Some(loginAttempt) =>
        userService.login(loginAttempt).map {
          case true =>
            Ok("Successfully logged in")
          case false =>
            BadRequest("Your username or password is incorrect")
        }
      case None =>
        Future.successful(BadRequest)
    }
  }
}
