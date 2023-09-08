import exceptions.ValidationException
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.Singleton

@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.obj("message" -> s"A client error occurred: {$message}"))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case _: ValidationException => Future.successful(BadRequest(exception.getMessage))
      case _ => Future.successful(InternalServerError(exception.getMessage))
    }
  }
}