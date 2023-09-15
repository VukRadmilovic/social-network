package controllers

import actions.JWTAuthAction
import dtos.{PaginatedResult, UserDTO}
import helpers.RequestKeys.TokenUsername
import models.FriendRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import services.{FriendRequestService, UserService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FriendController @Inject() (
    val controllerComponents: ControllerComponents,
    val userService: UserService,
    val friendRequestService: FriendRequestService,
    val jwtAuthAction: JWTAuthAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val paginatedResultUserJsonFormat: OFormat[PaginatedResult[UserDTO]] = Json.format[PaginatedResult[UserDTO]]
  implicit val paginatedResultFriendRequestJsonFormat: OFormat[PaginatedResult[FriendRequest]] = Json.format[PaginatedResult[FriendRequest]]

  private def resolveRequest(id: Long, serviceAction: (Long, String) => Future[Unit]): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      serviceAction(id, username).map(_ => NoContent)
    }

  def cancelRequest(id: Long): Action[AnyContent] =
    resolveRequest(id, friendRequestService.cancelRequest)

  def rejectRequest(id: Long): Action[AnyContent] =
    resolveRequest(id, friendRequestService.rejectRequest)

  def acceptRequest(id: Long): Action[AnyContent] =
    resolveRequest(id, friendRequestService.acceptRequest)

  private def getRequests(serviceAction: (String, Long, Long) => Future[PaginatedResult[FriendRequest]]): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(10L)
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      serviceAction(username, limit, page).map(requests => Ok(Json.toJson(requests)))
    }

  def getReceivedRequests: Action[AnyContent] =
    getRequests(friendRequestService.getByReceiver)

  def getSentRequests: Action[AnyContent] =
    getRequests(friendRequestService.getBySender)

  def getFriends: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      val limit: Long = request.getQueryString("limit").map(_.toLong).getOrElse(10L)
      val page: Long = request.getQueryString("page").map(_.toLong).getOrElse(0L)

      userService
        .getFriendsPaginated(username, limit, page)
        .map(friends => Ok(Json.toJson(PaginatedResult
        (friends.totalCount, friends.entries.map(UserDTO(_)), friends.hasNextPage))))
    }

  def sendRequest(username: String): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val senderUsername = request.attrs.get(TokenUsername).get
      val friendRequest = FriendRequest.create(senderUsername, username)
      friendRequestService
        .sendRequest(friendRequest)
        .map(_ => NoContent)
    }
}
