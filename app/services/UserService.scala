package services

import dtos.LoginAttempt
import exceptions.AlreadyFriendsException
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
  def getAll: Future[Seq[User]] = userRepository.getAll

  def getByUsername(username: String): Future[Option[User]] =
    userRepository.getByUsername(username)

  def getByEmail(email: String): Future[Option[User]] =
    userRepository.getByEmail(email)

  def register(user: User): Future[User] = {
    userValidationService
      .validate(user)
      .flatMap(_ => {
        val newUser = User(user.username, user.displayName, Cryptography.hashPassword(user.password), user.email)
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

  def addFriends(username1: String, username2: String): Future[Unit] = {
    userRepository.areFriends(username1, username2).map(friends => {
      if (friends) {
        throw AlreadyFriendsException()
      } else {
        userRepository.addFriends(username1, username2)
          .recover(e => throw e)
      }
    })
  }

  def getFriends(username: String): Future[Seq[String]] = {
    userRepository.getFriends(username)
  }
}
