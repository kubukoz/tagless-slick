package com.kubukoz.slick.algebra
import com.kubukoz.slick.interpreter.DBIOInterpreter
import slick.lifted.Query

trait SelectAlgebra[F[_], E] {
  def all(query: Query[_, E, Seq]): F[List[E]]
}

object SelectAlgebra {
  implicit def deriveSelectAlgebra[F[_]: DBIOInterpreter, E]: SelectAlgebra[F, E] = new InterpSelectAlgebra[F, E] {
    override protected def interpreter: DBIOInterpreter[F] = DBIOInterpreter[F]
  }
}

private[slick] abstract class InterpSelectAlgebra[F[_], E] extends SelectAlgebra[F, E] {
  protected def interpreter: DBIOInterpreter[F]

  override def all(query: Query[_, E, Seq]): F[List[E]] = interpreter.withApi { api =>
    import api._

    interpreter.interpret(query.to[List].result)
  }
}
