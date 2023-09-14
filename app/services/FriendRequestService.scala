package services

import exceptions.{AuthorizationException, NotFoundException, ValidationException}
import models.FriendRequest
import models.RequestResolution.{Accept, Cancel, Reject, RequestResolution}
import models.RequestStatus.Pending
import play.api.Logging
import repositories.{FriendRequestRepository, UserRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FriendRequestService @Inject() (
    friendRequestRepository: FriendRequestRepository,
    userRepository: UserRepository
)(implicit ec: ExecutionContext)
    extends Logging {
  def getByReceiver(username: String): Future[Seq[FriendRequest]] = {
    friendRequestRepository.getByReceiver(username)
  }

  def getBySender(username: String): Future[Seq[FriendRequest]] = {
    friendRequestRepository.getBySender(username)
  }

  private def validate(id: Long, username: String, resolution: RequestResolution): Future[FriendRequest] = {
    friendRequestRepository.getFriendRequestById(id).flatMap {
      case Some(request) if request.status == Pending =>
        val (supposedUsername, failMsg) = resolution match {
          case Cancel => (request.sender, "You can only cancel your own requests")
          case _      => (request.receiver, s"You can only ${resolution.toString.toLowerCase} requests you received")
        }

        if (supposedUsername == username) {
          Future.successful(request)
        } else {
          Future.failed(AuthorizationException(failMsg))
        }
      case Some(_) =>
        Future.failed(ValidationException(
          s"You can only ${resolution.toString.toLowerCase} requests which are still pending")
        )
      case None =>
        Future.failed(NotFoundException("This friend request does not exist"))
    }
  }

  def rejectRequest(id: Long, username: String): Future[Unit] = {
    validate(id, username, Reject).flatMap { _ =>
      friendRequestRepository.reject(id)
    }
  }

  def acceptRequest(id: Long, username: String): Future[Unit] = {
    validate(id, username, Accept).flatMap { request =>
      friendRequestRepository.accept(id).flatMap { _ =>
        userRepository.addFriends(request.sender, request.receiver)
      }
    }
  }

  def cancelRequest(id: Long, username: String): Future[Unit] = {
    validate(id, username, Cancel).flatMap { _ =>
      friendRequestRepository.cancel(id)
    }
  }

  def sendRequest(friendRequest: FriendRequest): Future[Unit] = {
    for {
      _ <- validateNotSelf(friendRequest)
      _ <- validateNoPendingRequestFrom(friendRequest)
      _ <- validateNoPendingRequestTo(friendRequest)
      targetUser <- validateUserExists(friendRequest)
      _ <- validateNotAlreadyFriends(friendRequest.sender, targetUser)
      _ <- friendRequestRepository.send(friendRequest)
    } yield ()
  }

  private def validateNotSelf(friendRequest: FriendRequest): Future[Unit] = {
    if (friendRequest.sender == friendRequest.receiver) {
      Future.failed(ValidationException("You cannot add yourself as a friend"))
    } else {
      Future.successful(())
    }
  }

  private def validateNoPendingRequestFrom(friendRequest: FriendRequest): Future[Unit] = {
    friendRequestRepository.pendingRequestToSenderExists(friendRequest).flatMap {
      case true => Future.failed(ValidationException("You already have a pending friend request from this person"))
      case _ => Future.successful(())
    }
  }

  private def validateNoPendingRequestTo(friendRequest: FriendRequest): Future[Unit] = {
    friendRequestRepository.pendingRequestFromSenderExists(friendRequest).flatMap {
      case true => Future.failed(ValidationException("You already have a pending friend request to this person"))
      case _ => Future.successful(())
    }
  }

  private def validateUserExists(friendRequest: FriendRequest): Future[String] = {
    userRepository.getByUsername(friendRequest.receiver).flatMap {
      case Some(user) => Future.successful(user.username)
      case None => Future.failed(NotFoundException("The user you are trying to add does not exist"))
    }
  }

  private def validateNotAlreadyFriends(sender: String, receiver: String): Future[Unit] = {
    userRepository.areFriends(sender, receiver).flatMap {
      case true => Future.failed(ValidationException("You are already friends"))
      case _ => Future.successful(())
    }
  }
}
