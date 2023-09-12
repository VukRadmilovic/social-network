package exceptions

final case class AuthorizationException(
                                      private val message: String = "",
                                      private val cause: Throwable = None.orNull
                                    ) extends Exception(message, cause)