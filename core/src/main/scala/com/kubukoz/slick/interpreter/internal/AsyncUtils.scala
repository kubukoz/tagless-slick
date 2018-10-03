package com.kubukoz.slick.interpreter.internal
import cats.effect.{Async, Sync}

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._

private[interpreter] object AsyncUtils {

  def fromFuture[F[_]: Async, A](f: F[Future[A]]): F[A] = {
    //only for `onComplete` - non-blocking call
    import ExecutionContext.Implicits.global

    f.flatMap { future =>
      future.value match {
        case Some(valueTry) => Sync[F].fromTry(valueTry)
        case None =>
          Async[F].async(cb => future.onComplete(tried => cb(tried.toEither)))
      }
    }
  }
}
