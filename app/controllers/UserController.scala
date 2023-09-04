package controllers

import models.User

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import scala.annotation.unused
import scala.collection.mutable

@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val users = new mutable.ListBuffer[User]()
  users += User("jova", "123")
  users += User("nika", "456")

  implicit val usersJson: OFormat[User] = Json.format[User]

  def getAll: Action[AnyContent] = Action {
    if (users.isEmpty) {
      NoContent
    } else {
      Ok(Json.toJson(users))
    }
  }

  def getByUsername(username: String): Action[AnyContent] = Action {
    val foundUser = users.find(_.username == username)
    foundUser match {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }
}
