package services

import dtos.{LoginAttemptDTO, PaginatedResult}
import exceptions.{AuthorizationException, ValidationException}
import helpers.MinIO
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
  def getAll(limit: Long, page: Long): Future[PaginatedResult[User]] =
    userRepository.getAll(limit, page)

  /**
   * Searches for users whose usernames or display names match a given search term using a full-text search.
   *
   * This method delegates the search operation to the UserRepository, which performs a case-insensitive search
   * for users based on usernames or display names containing words similar to the provided search term.
   * It returns a paginated list of matching users, along with total count and pagination information.
   *
   * @param name  The search term used to filter users based on usernames or display names.
   * @param limit The maximum number of users to retrieve on each page.
   * @param page  The page number for paginating the search results (starting from 0).
   * @return A Future containing a paginated list of users whose usernames or display names match the search term.
   */
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
    userRepository.hasProfilePicture(pictureOwner).map { hasProfilePicture =>
      if (hasProfilePicture.get) {
        MinIO.getProfilePicture(pictureOwner)
      } else {
        MinIO.getProfilePicture("default.jpg")
      }
    }
  }

  def uploadProfilePicture(username: String, file: File, contentType: String): Future[Unit] = {
    MinIO.uploadProfilePicture(username, file, contentType)
    userRepository.addProfilePicture(username)
  }

  def deleteProfilePicture(username: String): Future[Unit] = {
    userRepository.hasProfilePicture(username).flatMap {
      case Some(has) if has =>
        MinIO.deleteProfilePicture(username)
        userRepository.deleteProfilePicture(username)
      case _ =>
        throw ValidationException("You don't have a profile picture")
    }
  }
}
