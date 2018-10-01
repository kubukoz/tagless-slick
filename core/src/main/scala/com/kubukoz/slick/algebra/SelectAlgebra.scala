package com.kubukoz.slick.algebra
import com.kubukoz.slick.interpreter.DBIOInterpreter
import simulacrum.typeclass
import slick.lifted.{Query => LiftedQuery}

@typeclass
trait SelectAlgebra[F[_]] {
  def result[T, E, S[_]](query: LiftedQuery[T, E, S]): F[S[E]]
}

object SelectAlgebra {
  implicit def derive[F[_]: DBIOInterpreter]: SelectAlgebra[F] = new InterpSelectAlgebra[F] {
    override protected def interpreter: DBIOInterpreter[F] = DBIOInterpreter[F]
  }
}

private[slick] abstract class InterpSelectAlgebra[F[_]] extends SelectAlgebra[F] {
  protected def interpreter: DBIOInterpreter[F]

  override def result[T, E, S[_]](query: LiftedQuery[T, E, S]): F[S[E]] = interpreter.withApi { api =>
    import api._

    interpreter.eval(query.result)
  }
}
