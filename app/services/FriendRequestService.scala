package services

import exceptions.ValidationException
import models.FriendRequest
import repositories.{FriendRequestRepository, UserRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FriendRequestService @Inject() (
    friendRequestRepository: FriendRequestRepository,
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {
  //TODO: refactor?
  def sendRequest(friendRequest: FriendRequest): Future[FriendRequest] = {
    if (friendRequest.sender == friendRequest.receiver) {
      Future.failed(ValidationException("You cannot add yourself as a friend"))
    } else {
      friendRequestRepository
        .pendingRequestFromSenderExists(friendRequest)
        .flatMap { exists =>
          if (exists) {
            Future.failed(
              ValidationException(
                "You already have a pending friend request to this person"
              )
            )
          } else {
            friendRequestRepository
              .pendingRequestToSenderExists(friendRequest)
              .flatMap { exists =>
                if (exists) {
                  Future.failed(
                    ValidationException(
                      "You already have a pending friend request from this person"
                    )
                  )
                } else {
                  userRepository.getByUsername(friendRequest.receiver).flatMap {
                    case Some(_) =>
                      userRepository
                        .areFriends(
                          friendRequest.sender,
                          friendRequest.receiver
                        )
                        .flatMap { friends =>
                          if (friends) {
                            Future.failed(
                              ValidationException("You are already friends")
                            )
                          } else {
                            friendRequestRepository.send(friendRequest)
                          }
                        }
                    case None =>
                      Future.failed(
                        ValidationException(
                          "The user you are trying to add does not exist"
                        )
                      )
                  }
                }
              }
          }
        }
    }
  }
}
