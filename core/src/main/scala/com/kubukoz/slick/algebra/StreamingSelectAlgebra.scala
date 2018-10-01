package com.kubukoz.slick.algebra

import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import fs2.Stream
import slick.lifted.Query

trait StreamingSelectAlgebra[F[_], E] extends SelectAlgebra[F, E] {
  def stream[T](query: Query[T, E, Seq]): Stream[F, E]
}

object StreamingSelectAlgebra {
  implicit def derive[F[_]: StreamingDBIOInterpreter, E]: StreamingSelectAlgebra[F, E] =
    new InterpStreamingAlgebra[F, E] {
      override protected def interpreter: StreamingDBIOInterpreter[F] = StreamingDBIOInterpreter[F]
    }
}

private[slick] abstract class InterpStreamingAlgebra[F[_], E]
    extends InterpSelectAlgebra[F, E]
    with StreamingSelectAlgebra[F, E] {
  protected def interpreter: StreamingDBIOInterpreter[F]

  override def stream[T](query: Query[T, E, Seq]): Stream[F, E] = interpreter.withApi.apply { api =>
    import api._

    interpreter.stream(query.result)
  }
}
