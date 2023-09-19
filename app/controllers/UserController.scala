package controllers

import actions.JWTAuthAction
import dtos._
import exceptions.ValidationException
import helpers.RequestKeys.TokenUsername
import models.User
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import play.api.{Configuration, Logging}
import repositories.UserRepository
import services.{AuthService, FriendRequestService, UserService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

//noinspection ScalaDocUnknownParameter
@Singleton
class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    val userRepository: UserRepository,
    val userService: UserService,
    val friendRequestService: FriendRequestService,
    val jwtAuthAction: JWTAuthAction,
    val authService: AuthService,
    val configuration: Configuration
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val paginatedResultUserJsonFormat: OFormat[PaginatedResult[OutputUserDTO]] = Json.format[PaginatedResult[OutputUserDTO]]

  def getAll: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(configuration.get[Long]("entriesPerPage"))
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      userService.getAll(limit, page).map(users => {
        Ok(Json.toJson(PaginatedResult(users.totalCount, users.entries.map(OutputUserDTO(_)), users.hasNextPage)))
      })
    }

  /**
   * Search for users whose usernames or display names match a specified string using a MySQL full-text search in a paginated manner.
   *
   * This endpoint performs a MySQL full-text search on the usernames and display names of users,
   * matching them against the provided string, and returns the results in a paginated JSON format.
   * Pagination parameters (limit and page) can be optionally provided to control the number of results
   * and the page number.
   *
   * @return A JSON response containing users whose usernames or display names match the provided string
   *         using MySQL full-text search, along with total count and pagination information.
   */
  def search: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val name: String = request.getQueryString("name").getOrElse(throw ValidationException("No name to search for has been provided"))
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(configuration.get[Long]("entriesPerPage"))
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      userService.search(name, limit, page).map(users => {
        Ok(Json.toJson(PaginatedResult(users.totalCount, users.entries.map(OutputUserDTO(_)), users.hasNextPage)))
      })
    }

  def getByUsername(username: String): Action[AnyContent] =
    jwtAuthAction.async {
      userService.getByUsername(username).map {
        case Some(user) => Ok(Json.toJson(OutputUserDTO(user)))
        case None => NotFound
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

  def register(): Action[InputUserDTO] =
    Action.async(parse.json[InputUserDTO]) { implicit request =>
      val user = request.body

      userService.register(User.create(user)).map(newUser => Created(Json.toJson(OutputUserDTO(newUser))))
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

  /**
   * Retrieves the URL of the profile picture for a specified user.
   *
   * This endpoint returns a URL that can be used to access the profile picture of the specified user.
   * The URL is generated using a presigned URL from MinIO, ensuring secure access to the image.
   *
   * @param pictureOwner The username of the user whose profile picture URL is to be retrieved.
   * @return A JSON response containing the URL of the profile picture.
   */
  def getProfilePicture(pictureOwner: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      userService.getProfilePicture(username, pictureOwner).map(picture => Ok(Json.toJson(picture)))
    }

  def uploadProfilePicture: Action[MultipartFormData[Files.TemporaryFile]] =
    jwtAuthAction(parse.multipartFormData).async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      request.body.file("picture") match {
        case Some(picture) =>
          val file = picture.ref.path.toFile
          val fileType = picture.contentType

          userService.uploadProfilePicture(username, file, fileType.get).map(_ => NoContent)
        case None => Future.failed(ValidationException("Missing picture"))
      }
    }

  def deleteProfilePicture(): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get

      userService.deleteProfilePicture(username).map(_ => NoContent)
    }
}
