package services

import exceptions.AuthorizationException
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.Configuration
import play.api.libs.json.Json

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class AuthService @Inject() (configuration: Configuration)(implicit ec: ExecutionContext) {
  implicit val clock: Clock = Clock.systemUTC
  private val secretKey = "01c0d934ee75f196cdfed19207a549aa60fcac4194011602e4b12f7b1cd5e17e"

  private val accessTokenExpirationMillis = configuration.get[FiniteDuration]("accessTokenExpiration").toMillis
  private val refreshTokenExpirationMillis = configuration.get[FiniteDuration]("refreshTokenExpiration").toMillis

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

  private def generateToken(username: String, expirationMillis: Long): String = {
    val claim = Json.obj(("sub", username), ("exp", clock.millis() + expirationMillis))
    JwtJson.encode(claim, secretKey, JwtAlgorithm.HS256)
  }

  private def generateAccessToken(username: String): String = {
    generateToken(username, accessTokenExpirationMillis)
  }

  private def generateRefreshToken(username: String): String = {
    generateToken(username, refreshTokenExpirationMillis)
  }

  def generateTokens(username: String): (String, String) = {
    (generateAccessToken(username), generateRefreshToken(username))
  }

  def generateAccessTokenIfRefreshValid(refreshToken: String): Future[String] = {
    validateToken(refreshToken).map {
      case Some(username) => generateAccessToken(username)
      case None => throw AuthorizationException("Authentication failed: invalid or expired token")
    }
  }
}
