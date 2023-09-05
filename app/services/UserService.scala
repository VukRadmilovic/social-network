package services

import dtos.{LoginAttempt, NewUser}
import helpers.Cryptography
import models.User
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import repositories.UserRepository

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}


class UserService @Inject() (userRepository: UserRepository,
                             authService: AuthService) (implicit ec: ExecutionContext) {
  def getAll: Future[ListBuffer[User]] = userRepository.getAll

  def getByUsername(username: String): Future[Option[User]] = userRepository.getByUsername(username)

  def getByEmail(email: String): Future[Option[User]] = userRepository.getByEmail(email)

  def register(user: NewUser): Future[Either[Result, User]] = {
    if (user.username.isBlank || user.email.isBlank || user.displayName.isBlank || user.password.isBlank) {
      return Future.successful(Left(BadRequest(Json.obj("message" -> "Please enter all the data"))))
    }

    val usernameFuture = userRepository.getByUsername(user.username)
    val emailFuture = userRepository.getByEmail(user.email)

    for {
      usernameOption <- usernameFuture
      emailOption <- emailFuture
    } yield {
      (usernameOption, emailOption) match {
        case (Some(_), _) =>
          Left(BadRequest(Json.obj("message" -> "Username is already in use")))
        case (_, Some(_)) =>
          Left(BadRequest(Json.obj("message" -> "Email is already in use")))
        case (None, None) =>
          val newUser = new User(user, Cryptography.hashPassword(user.password))
          userRepository.create(newUser)
          Right(newUser)
      }
    }
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
