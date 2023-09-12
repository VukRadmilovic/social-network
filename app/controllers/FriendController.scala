package controllers

import actions.JWTAuthAction
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

  private def getRequests(serviceAction: String => Future[Seq[FriendRequest]]): Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      serviceAction(username).map(requests => Ok(Json.toJson(requests)))
    }

  def getReceivedRequests: Action[AnyContent] =
    getRequests(friendRequestService.getByReceiver)

  def getSentRequests: Action[AnyContent] =
    getRequests(friendRequestService.getBySender)

  def getFriends: Action[AnyContent] =
    jwtAuthAction.async { implicit request =>
      val username = request.attrs.get(TokenUsername).get
      userService
        .getFriends(username)
        .map(friends => Ok(Json.toJson(friends)))
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
