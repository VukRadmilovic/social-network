package controllers

import dtos.NewUser
import models.User

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import repositories.UserRepository


// TODO: use database
@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents,
                               val userRepository: UserRepository) extends BaseController {
  implicit val usersJson: OFormat[User] = Json.format[User]
  implicit val newUsersJson: OFormat[NewUser] = Json.format[NewUser]

  def getAll: Action[AnyContent] = Action {
    val users = userRepository.getAll
    if (users.isEmpty) {
      NoContent
    } else {
      Ok(Json.toJson(users))
    }
  }

  def getByUsername(username: String): Action[AnyContent] = Action {
    val foundUser = userRepository.getByUsername(username)
    foundUser match {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }

  def register(): Action[AnyContent] = Action { implicit request =>
    val body = request.body.asJson
    val user: Option[NewUser] =
      body.flatMap(
        Json.fromJson[NewUser](_).asOpt
      )

    user match {
      case Some(user) =>
        if (userRepository.getByUsername(user.username).isDefined) {
          BadRequest(Json.obj("message" -> "Username is already in use"))
        } else if (userRepository.getByEmail(user.email).isDefined) {
          BadRequest(Json.obj("message" -> "Email is already in use"))
        } else {
          val newUser = new User(user)
          Created(Json.toJson(userRepository.create(newUser)))
        }
      case None =>
        BadRequest
    }
  }
}
