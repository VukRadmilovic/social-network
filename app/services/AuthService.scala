package services

import pdi.jwt.{JwtAlgorithm, JwtJson}

import javax.inject.Inject
import play.api.libs.json.Json
import repositories.UserRepository

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class AuthService @Inject() (userRepository: UserRepository)(implicit ec: ExecutionContext) {
  implicit val clock: Clock = Clock.systemUTC
  private val secretKey = "01c0d934ee75f196cdfed19207a549aa60fcac4194011602e4b12f7b1cd5e17e"

  def validateToken(token: String): Future[Option[String]] = {
    try {
      val claim = JwtJson.decode(token, secretKey, Seq(JwtAlgorithm.HS256))
      val username = claim.get.subject.get
      val expiry = claim.get.expiration.get
      if (System.currentTimeMillis() - expiry > 0) {
        return Future.successful(None)
      }
      userRepository.getByUsername(username).map {
        case Some(_) => Some(username)
        case None => None
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
