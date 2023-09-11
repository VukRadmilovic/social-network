import exceptions.ValidationException
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logging

import scala.concurrent._
import javax.inject.Singleton

@Singleton
class ErrorHandler extends HttpErrorHandler with Logging {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.obj("message" -> s"A client error occurred: {$message}"))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case _: ValidationException => Future.successful(BadRequest(Json.obj("message" -> exception.getMessage)))
      case _ =>
        logger.error("Error caught in global error handler", exception)
        Future.successful(InternalServerError)
    }
  }
}