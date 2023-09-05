package helpers

import play.api.libs.typedmap.TypedKey

object RequestKeys {
  val TokenUsername: TypedKey[String] = TypedKey[String]("tokenUsername")
}
