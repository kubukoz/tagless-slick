package com.kubukoz.slick.interpreter.internal
import cats.effect.{Async, Sync}

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._

private[interpreter] object AsyncUtils {

  def fromFuture[F[_]: Async, A](f: F[Future[A]]): F[A] = {
    //only for `onComplete` - non-blocking call
    import ExecutionContext.Implicits.global

    Async[F].asyncF { cb =>
      f.flatMap { future =>
        Sync[F].delay {
          future.value match {
            case Some(valueTry) => cb(valueTry.toEither)
            case None           => future.onComplete(tried => cb(tried.toEither))
          }
        }
      }
    }
  }
}
