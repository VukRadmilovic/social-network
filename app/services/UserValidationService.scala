package services

import exceptions.ValidationException
import models.User
import repositories.UserRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserValidationService @Inject() (
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {
  private def missingData(user: User): Boolean = {
    user.username.isBlank || user.email.isBlank || user.displayName.isBlank || user.password.isBlank
  }

  private def usernameInUse(username: String): Future[Boolean] = {
    userRepository.getByUsername(username).map {
      case Some(_) => true
      case None    => false
    }
  }

  private def emailInUse(email: String): Future[Boolean] = {
    userRepository.getByEmail(email).map {
      case Some(_) => true
      case None    => false
    }
  }

  def validate(user: User): Future[Unit] = {
    if (missingData(user)) {
      Future.failed(ValidationException("Missing data"))
    } else {
      usernameInUse(user.username).flatMap(used => {
        if (used) {
          Future.failed(ValidationException("Username is already in use"))
        } else {
          emailInUse(user.email).flatMap(used => {
            if (used) {
              Future.failed(ValidationException("Email is already in use"))
            } else {
              Future.successful(())
            }
          })
        }
      })
    }
  }
}
