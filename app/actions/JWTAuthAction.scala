package actions

import helpers.RequestKeys.TokenUsername
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import services.AuthService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JWTAuthAction @Inject()(
                                       parser: BodyParsers.Default,
                                       authService: AuthService
                                     )(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
    request.headers.get("Authorization") match {
      case Some(token) =>
        authService
          .validateToken(token)
          .flatMap {
            case Some(username) =>
              val requestWithTokenUsername = request.addAttr(TokenUsername, username)
              block(requestWithTokenUsername)
            case None => Future.successful(Unauthorized(Json.obj(
              "message" -> "Authentication failed: invalid or expired token")))
          }
      case _ =>
        Future.successful(Unauthorized(Json.obj("message" -> "Authentication failed: missing token")))
    }
}
