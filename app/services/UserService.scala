package services

import dtos.{LoginAttempt, UserWithFriends}
import helpers.Cryptography
import models.User
import repositories.UserRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (
    userRepository: UserRepository,
    authService: AuthService,
    userValidationService: UserValidationService
)(implicit ec: ExecutionContext) {
  def getAll: Future[Seq[UserWithFriends]] = userRepository.getAll

  def getByUsername(username: String): Future[Option[UserWithFriends]] =
    userRepository.getByUsername(username)

  def getByEmail(email: String): Future[Option[UserWithFriends]] =
    userRepository.getByEmail(email)

  def register(user: User): Future[UserWithFriends] = {
    userValidationService
      .validate(user)
      .flatMap(_ => {
        val newUser = new UserWithFriends(
          user,
          Cryptography.hashPassword(user.password)
        )
        userRepository.create(newUser)
      })
      .recoverWith(e => {
        Future.failed(e)
      })
  }

  def login(loginAttempt: LoginAttempt): Future[Option[String]] = {
    val userFuture = userRepository.getByUsername(loginAttempt.username)
    userFuture.map {
      case Some(user) =>
        if (Cryptography.checkPassword(loginAttempt.password, user.password)) {
          val token = authService.generateToken(user.username)
          Some(token)
        } else {
          None
        }
      case None =>
        None
    }
  }
}
