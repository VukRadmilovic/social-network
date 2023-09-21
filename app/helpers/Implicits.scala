package helpers

import java.util.concurrent.CompletableFuture
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

object Implicits {
  implicit class CompletableFutureOps[T](future: CompletableFuture[T]) {
    def toScala: Future[T] = FutureConverters.toScala(future)
  }
}