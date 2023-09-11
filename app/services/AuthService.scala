package services

import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json.Json

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthService @Inject() (implicit ec: ExecutionContext) {
  implicit val clock: Clock = Clock.systemUTC
  private val secretKey = "01c0d934ee75f196cdfed19207a549aa60fcac4194011602e4b12f7b1cd5e17e"

  def validateToken(token: String): Future[Option[String]] = {
    try {
      val claim = JwtJson.decode(token, secretKey, Seq(JwtAlgorithm.HS256))
      val username = claim.get.subject.get
      val expiry = claim.get.expiration.get
      if (System.currentTimeMillis() - expiry > 0) {
        Future.successful(None)
      } else {
        Future.successful(Some(username))
      }
    } catch {
      case _: Throwable => Future.successful(None)
    }
  }

  def generateToken(username: String): String = {
    val SixtyMinutes = 60 * 60 * 1000

    val expirationTime = clock.millis() + SixtyMinutes
    val claim = Json.obj(("sub", username), ("exp", expirationTime))
    JwtJson.encode(claim, secretKey, JwtAlgorithm.HS256)
  }
}
