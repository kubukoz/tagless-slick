package com.kubukoz.slick.interpreter

import cats.arrow.FunctionK
import cats.effect.ConcurrentEffect
import cats.tagless.finalAlg
import cats.~>
import fs2.Stream
import slick.dbio.{DBIO, StreamingDBIO}
import slick.jdbc.JdbcProfile

@finalAlg
trait StreamingDBIOInterpreter[F[_]] extends DBIOInterpreter[F] {
  def stream[R]: StreamingDBIO[R, ?] ~> Stream[F, ?]

  def withPreparation(preInterpret: DBIO ~> DBIO): StreamingDBIOInterpreter[F]
}

object StreamingDBIOInterpreter {

  def streamingInterpreter[F[_]: ConcurrentEffect](profile: JdbcProfile,
                                                   db: JdbcProfile#API#Database): StreamingDBIOInterpreter[F] =
    new ConcurrentEffectStreamingDBIOInterpreter[F](profile, db, FunctionK.id) {
      override protected val F: ConcurrentEffect[F] = ConcurrentEffect[F]
    }
}

abstract class ConcurrentEffectStreamingDBIOInterpreter[F[_]](profile: JdbcProfile,
                                                              db: JdbcProfile#API#Database,
                                                              rawTrans: DBIO ~> DBIO)
    extends AsyncInterpreter[F](profile, db, FunctionK.id)
    with StreamingDBIOInterpreter[F] { self =>

  override protected def F: ConcurrentEffect[F]

  private def streamConvert[R, A](dbio: StreamingDBIO[R, A]): Stream[F, A] =
    fs2.interop.reactivestreams.fromPublisher[F, A](db.stream(dbio))(F)

  override def stream[R]: StreamingDBIO[R, ?] ~> Stream[F, ?] =
    λ[StreamingDBIO[R, ?] ~> Stream[F, ?]](streamConvert(_))

  override def withPreparation(preInterpret: DBIO ~> DBIO): StreamingDBIOInterpreter[F] =
    new ConcurrentEffectStreamingDBIOInterpreter[F](profile, db, preInterpret.andThen(rawTrans)) {
      override protected def F: ConcurrentEffect[F] = self.F
    }
}
