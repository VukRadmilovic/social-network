package services

import dtos.{LoginAttemptDTO, PaginatedResult}
import exceptions.{AuthorizationException, ValidationException}
import helpers.S3API
import models.User
import org.mindrot.jbcrypt.BCrypt
import repositories.UserRepository

import java.io.File
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (
    userRepository: UserRepository,
    authService: AuthService,
    userValidationService: UserValidationService
)(implicit ec: ExecutionContext) {
  private val profilePicturesBucket = "profile-pictures"

  def getAll(limit: Long, page: Long): Future[PaginatedResult[User]] =
    userRepository.getAll(limit, page)

  def search(name: String, limit: Long, page: Long): Future[PaginatedResult[User]] =
    userRepository.search(name, limit, page)

  def getByUsername(username: String): Future[Option[User]] =
    userRepository.getByUsername(username)

  def getByEmail(email: String): Future[Option[User]] =
    userRepository.getByEmail(email)

  def register(user: User): Future[User] = {
    userValidationService
      .validate(user)
      .flatMap(_ => {
        val newUser = User(user.username, user.displayName, BCrypt.hashpw(user.password, BCrypt.gensalt()), user.email, user.hasProfilePicture)
        userRepository.create(newUser)
      })
  }

  def login(loginAttempt: LoginAttemptDTO): Future[Option[(String, String)]] = {
    userRepository.getByUsername(loginAttempt.username).flatMap {
      case Some(user) if BCrypt.checkpw(loginAttempt.password, user.password) =>
        Future.successful(Some(authService.generateTokens(user.username)))
      case _ =>
        Future.successful(None)
    }
  }

  def getFriendsPaginated(username: String, limit: Long, page: Long): Future[PaginatedResult[User]] = {
    userRepository.getFriendsPaginated(username, limit, page)
  }

  def changeDisplayName(username: String, newDisplayName: String): Future[Unit] = {
    if (newDisplayName.isBlank) {
      Future.failed(ValidationException("Missing new display name"))
    } else {
      userRepository.changeDisplayName(username, newDisplayName)
    }
  }

  def changePassword(username: String, oldPassword: String, newPassword: String): Future[Unit] = {
    userRepository.getByUsername(username).flatMap {
      case Some(user) if BCrypt.checkpw(oldPassword, user.password) =>
        userRepository.changePassword(username, BCrypt.hashpw(newPassword, BCrypt.gensalt()))
      case _ =>
        Future.failed(AuthorizationException("Incorrect old password"))
    }
  }

  def changeEmail(username: String, currentPassword: String, newEmail: String): Future[Unit] = {
    userRepository.getByUsername(username).flatMap {
      case Some(user) if BCrypt.checkpw(currentPassword, user.password) =>
        if (newEmail.isBlank)
          Future.failed(ValidationException("Missing new email"))
        else
          userRepository.changeEmail(username, newEmail)
      case _ =>
        Future.failed(AuthorizationException("Incorrect current password"))
    }
  }

  def getProfilePicture(username: String, pictureOwner: String): Future[String] = {
    if (username == pictureOwner) {
      getProfilePicture(pictureOwner)
    } else {
      userRepository.getByUsername(pictureOwner).flatMap {
        case Some(_) => userRepository.areFriends(username, pictureOwner).flatMap { friends =>
          if (friends) {
            getProfilePicture(pictureOwner)
          } else {
            throw AuthorizationException("You can only see friend's profile pictures")
          }
        }
        case None => throw ValidationException("User does not exist")
      }
    }
  }

  private def getProfilePicture(pictureOwner: String): Future[String] = {
    userRepository.hasProfilePicture(pictureOwner).flatMap { hasProfilePicture =>
      if (hasProfilePicture.get) {
        S3API.get(profilePicturesBucket, pictureOwner)
      } else {
        S3API.get(profilePicturesBucket, "default.jpg")
      }
    }
  }

  def uploadProfilePicture(username: String, file: File, contentType: String): Future[Unit] = {
    S3API.put(profilePicturesBucket, username, file, contentType).flatMap(_ => {
      userRepository.addProfilePicture(username)
    })
  }

  def deleteProfilePicture(username: String): Future[Unit] = {
    userRepository.hasProfilePicture(username).flatMap {
      case Some(has) if has =>
        S3API.delete(profilePicturesBucket, username).map(_ => {
          userRepository.deleteProfilePicture(username)
        })
      case _ =>
        throw ValidationException("You don't have a profile picture")
    }
  }
}
