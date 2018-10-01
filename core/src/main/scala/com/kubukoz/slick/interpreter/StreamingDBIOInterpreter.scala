package com.kubukoz.slick.interpreter

import cats.effect.{Async, ConcurrentEffect}
import cats.~>
import fs2.Stream
import simulacrum.typeclass
import slick.dbio.StreamingDBIO
import slick.jdbc.JdbcProfile

@typeclass
trait StreamingDBIOInterpreter[F[_]] extends DBIOInterpreter[F] {
  def stream[R]: StreamingDBIO[R, ?] ~> Stream[F, ?]
}

object StreamingDBIOInterpreter {

  def streamingInterpreter[F[_]: ConcurrentEffect](profile: JdbcProfile,
                                                   db: JdbcProfile#API#Database): StreamingDBIOInterpreter[F] =
    new AsyncInterpreter[F](profile, db) with StreamingDBIOInterpreter[F] {
      override protected def F: Async[F] = ConcurrentEffect[F]

      override def stream[R]: StreamingDBIO[R, ?] ~> Stream[F, ?] = new (StreamingDBIO[R, ?] ~> Stream[F, ?]) {
        override def apply[A](fa: StreamingDBIO[R, A]): Stream[F, A] =
          fs2.interop.reactivestreams.fromPublisher[F, A](db.stream(fa))
      }
    }
}
