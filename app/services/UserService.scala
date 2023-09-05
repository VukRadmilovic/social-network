package services

import dtos.NewUser
import models.User
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import repositories.UserRepository

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (userRepository: UserRepository) (implicit ec: ExecutionContext) {
  def getAll: Future[ListBuffer[User]] = userRepository.getAll

  def getByUsername(username: String): Future[Option[User]] = userRepository.getByUsername(username)

  def getByEmail(email: String): Future[Option[User]] = userRepository.getByEmail(email)

  def register(user: NewUser): Future[Either[Result, User]] = {
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
          val newUser = new User(user)
          userRepository.create(newUser)
          Right(newUser)
      }
    }
  }
}
