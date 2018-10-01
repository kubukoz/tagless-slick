package com.kubukoz.slick.algebra

import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import fs2.Stream
import slick.lifted.{Query => LiftedQuery}

trait StreamingSelectAlgebra[F[_]] extends SelectAlgebra[F] {
  def stream[T, E, S[_]](query: LiftedQuery[T, E, S]): Stream[F, E]
}

object StreamingSelectAlgebra {
  def apply[F[_]: StreamingSelectAlgebra]: StreamingSelectAlgebra[F] = implicitly

  implicit def derive[F[_]: StreamingDBIOInterpreter]: StreamingSelectAlgebra[F] = new InterpStreamingAlgebra[F] {
    override protected def interpreter: StreamingDBIOInterpreter[F] = StreamingDBIOInterpreter[F]
  }
}

private[slick] abstract class InterpStreamingAlgebra[F[_]]
    extends InterpSelectAlgebra[F]
    with StreamingSelectAlgebra[F] {
  protected def interpreter: StreamingDBIOInterpreter[F]

  override def stream[T, E, S[_]](query: LiftedQuery[T, E, S]): Stream[F, E] = interpreter.withApi.apply { api =>
    import api._

    interpreter.stream(query.result)
  }
}
