package services

import dtos.LoginAttemptDTO
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
  def getAll: Future[Seq[User]] = userRepository.getAll

  def getByUsernameOrDisplayNameStartsWith(name: String): Future[Seq[User]] =
    userRepository.getByUsernameOrDisplayNameStartsWith(name)

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

  def login(loginAttempt: LoginAttemptDTO): Future[Option[String]] = {
    userRepository.getByUsername(loginAttempt.username).flatMap {
      case Some(user) if BCrypt.checkpw(loginAttempt.password, user.password) =>
        Future.successful(Some(authService.generateToken(user.username)))
      case _ =>
        Future.successful(None)
    }
  }

  def getFriends(username: String): Future[Seq[String]] = {
    userRepository.getFriends(username)
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
