package controllers

import actions.JWTAuthAction
import dtos.{EmailChangeDTO, LoginAttemptDTO, PaginatedResult, PasswordChangeDTO, RefreshTokenDTO, UserDTO}
import helpers.RequestKeys.TokenUsername
import models.User
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import repositories.UserRepository
import services.{AuthService, FriendRequestService, UserService}

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    val userRepository: UserRepository,
    val userService: UserService,
    val friendRequestService: FriendRequestService,
    val jwtAuthAction: JWTAuthAction,
    val authService: AuthService
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val paginatedResultUserJsonFormat: OFormat[PaginatedResult[UserDTO]] = Json.format[PaginatedResult[UserDTO]]

  def getAll: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(10L)
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      userService.getAll(limit, page).map(users => {
        Ok(Json.toJson(PaginatedResult(users.totalCount, users.entries.map(UserDTO(_)), users.hasNextPage)))
      })
    }

  /**
   * Retrieves users whose display name or username starts with a specified string (case-insensitive) in a paginated manner.
   *
   * This method performs a case-insensitive search for users whose display name or username
   * starts with the provided string and paginates the results.
   *
   * @param name  The search term used to filter users.
   * @param limit The maximum number of users to retrieve in each page.
   * @param page  The page number for paginating the results (starting from 0).
   * @return JSON representation of users whose display name or username starts with `name`.
   */
  def search(name: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(10L)
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      userService.search(name, limit, page).map(users => {
        Ok(Json.toJson(PaginatedResult(users.totalCount, users.entries.map(UserDTO(_)), users.hasNextPage)))
      })
    }

  def getByUsername(username: String): Action[AnyContent] =
    jwtAuthAction.async {
      userService.getByUsername(username).map {
        case Some(user) => Ok(Json.toJson(UserDTO(user)))
        case None       => NotFound
      }
    }

  def changeDisplayName(newDisplayName: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      userService.changeDisplayName(username, newDisplayName).map(_ => NoContent)
    }

  def changePassword(): Action[PasswordChangeDTO] =
    jwtAuthAction.async(parse.json[PasswordChangeDTO]) { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val passwordChangeDTO = request.body

      userService.changePassword(username, passwordChangeDTO.oldPassword, passwordChangeDTO.newPassword)
        .map(_ => NoContent)
    }

  def changeEmail(): Action[EmailChangeDTO] =
    jwtAuthAction.async(parse.json[EmailChangeDTO]) { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val emailChangeDTO = request.body

      userService.changeEmail(username, emailChangeDTO.currentPassword, emailChangeDTO.newEmail)
        .map(_ => NoContent)
    }

  def register(): Action[User] =
    Action.async(parse.json[User]) { implicit request =>
      val user = request.body

      userService.register(user).map(newUser => Created(Json.toJson(UserDTO(newUser))))
  }

  def login(): Action[LoginAttemptDTO] =
    Action.async(parse.json[LoginAttemptDTO]) { implicit request =>
      val loginAttempt = request.body

      userService.login(loginAttempt).map {
        case Some((accessToken, refreshToken)) =>
          Ok(Json.obj("access_token" -> accessToken, "refresh_token" -> refreshToken))
        case None =>
          Unauthorized(Json.obj("message" -> "Your username or password is incorrect"))
      }
  }

  def getAccessToken: Action[RefreshTokenDTO] =
    Action.async(parse.json[RefreshTokenDTO]) { implicit request =>
      val refreshTokenDTO = request.body

      authService
        .generateAccessTokenIfRefreshValid(refreshTokenDTO.refreshToken)
        .map(token => Ok(Json.obj("token" -> token)))
    }
}
