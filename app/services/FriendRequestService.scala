package services

import models.FriendRequest
import repositories.FriendRequestRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FriendRequestService @Inject() (
    friendRequestRepository: FriendRequestRepository
    )(implicit ec: ExecutionContext) {
  def sendRequest(friendRequest: FriendRequest): Future[FriendRequest] = {
    friendRequestRepository.send(friendRequest)
  }
}
