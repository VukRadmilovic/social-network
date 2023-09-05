package helpers

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Cryptography {
  private val DefaultIterations = 10000
  private val random = new SecureRandom()

  private def generateSalt(): Array[Byte] = {
    val salt = new Array[Byte](16)
    random.nextBytes(salt)
    salt
  }

  private def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, 256)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    keyFactory.generateSecret(keySpec).getEncoded
  }

  def hashPassword(password: String): String = {
    val salt = generateSalt()
    val hash = pbkdf2(password, salt, DefaultIterations)
    val salt64 = Base64.getEncoder.encodeToString(salt)
    val hash64 = Base64.getEncoder.encodeToString(hash)

    s"$DefaultIterations:$hash64:$salt64"
  }

  def checkPassword(password: String, passwordHash: String): Boolean = {
    passwordHash.split(":") match {
      case Array(it, hash64, salt64) if it.forall(_.isDigit) =>
        val hash = Base64.getDecoder.decode(hash64)
        val salt = Base64.getDecoder.decode(salt64)

        val calculatedHash = pbkdf2(password, salt, it.toInt)
        calculatedHash.sameElements(hash)

      case other => sys.error("Bad password hash")
    }
  }
}
