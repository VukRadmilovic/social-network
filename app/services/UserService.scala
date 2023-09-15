package services

import dtos.{LoginAttemptDTO, PaginatedResult}
import exceptions.{AuthorizationException, ValidationException}
import models.User
import org.mindrot.jbcrypt.BCrypt
import repositories.UserRepository

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
   * Retrieves users whose display name or username starts with a string in a paginated manner. Case insensitive.
   *
   * This method performs a case-insensitive search for users whose display name or username
   * starts with the provided string.
   *
   * @param name  The search term used to filter users.
   * @param limit The maximum number of users to retrieve on each page.
   * @param page  The page number for paginating the results (starting from 0).
   * @return JSON representation of a paginated list of User objects matching the search criteria.
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
        val newUser = User(user.username, user.displayName, BCrypt.hashpw(user.password, BCrypt.gensalt()), user.email)
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
}
