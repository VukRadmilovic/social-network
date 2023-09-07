package exceptions

final case class AlreadyFriendsException(
                                      private val message: String = "These users are already friends",
                                      private val cause: Throwable = None.orNull
                                    ) extends Exception(message, cause)
