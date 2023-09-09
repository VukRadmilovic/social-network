package controllers

import actions.JWTAuthAction
import dtos.{LoginAttempt, UserDTO}
import exceptions.ValidationException
import helpers.RequestKeys.TokenUsername
import models.{FriendRequest, User}
import play.api.libs.json._
import play.api.mvc._
import repositories.UserRepository
import services.{FriendRequestService, UserService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    val userRepository: UserRepository,
    val userService: UserService,
    val friendRequestService: FriendRequestService,
    val jwtAuthAction: JWTAuthAction
)(implicit ec: ExecutionContext)
    extends BaseController {
  def getAll: Action[AnyContent] = jwtAuthAction.async {
    userService.getAll.map(users => {
      val userDTOs = users.map(UserDTO(_))
      Ok(Json.toJson(userDTOs))
    })
  }

  def getByUsername(username: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      userService.getByUsername(username).map {
        case Some(user) => Ok(Json.toJson(UserDTO(user)))
        case None       => NotFound
      }
    }

  def register(): Action[User] = Action.async(parse.json[User]) {
    implicit request =>
      val user = request.body

      userService
        .register(user)
        .map(newUser => Created(Json.toJson(UserDTO(newUser))))
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
      if (username1 == username2) {
        throw ValidationException("You cannot add yourself as a friend")
      }
      val requesterUsername = request.attrs.get(TokenUsername).getOrElse("")
      if (requesterUsername != username1) {
        Future.successful(Forbidden)
      } else {
        userService
          .addFriends(username1, username2)
          .map(_ => NoContent)
      }
    }

  def getFriends(username: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val requesterUsername = request.attrs.get(TokenUsername).getOrElse("")
      if (requesterUsername != username) {
        Future.successful(Forbidden)
      } else {
        userService.getFriends(username).map(friends => Ok(Json.toJson(friends)))
      }
  }

  def sendFriendRequest(username: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val senderUsername = request.attrs.get(TokenUsername).get
      val friendRequest = FriendRequest.create(senderUsername, username)
      friendRequestService.sendRequest(friendRequest)
        .map(returnedRequest => Ok(Json.toJson(returnedRequest)))
  }
}
