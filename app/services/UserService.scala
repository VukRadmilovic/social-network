package services

import dtos.LoginAttempt
import exceptions.ValidationException
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

  def login(loginAttempt: LoginAttempt): Future[Option[String]] = {
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
}
