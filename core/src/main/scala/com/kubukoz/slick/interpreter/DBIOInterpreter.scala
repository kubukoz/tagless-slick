package com.kubukoz.slick.interpreter

import cats.effect.Async
import cats.~>
import com.kubukoz.slick.interpreter.DBIOInterpreter.WithApi
import com.kubukoz.slick.interpreter.internal.AsyncUtils
import simulacrum.typeclass
import slick.dbio._
import slick.jdbc.JdbcProfile

@typeclass
trait DBIOInterpreter[F[_]] {
  def profile: JdbcProfile

  def eval: DBIO ~> F

  private[slick] final def withApi: WithApi = new WithApi(profile)
}

object DBIOInterpreter {

  def interpreter[F[_]: Async](profile: JdbcProfile, db: JdbcProfile#API#Database): DBIOInterpreter[F] = {
    new AsyncInterpreter[F](profile, db) {
      override protected def F: Async[F] = Async[F]
    }
  }

  final class WithApi private[DBIOInterpreter] (val profile: JdbcProfile) extends AnyVal {
    def apply[A](f: profile.API => A): A = f(profile.api)
  }
}

private[interpreter] abstract class AsyncInterpreter[F[_]](val profile: JdbcProfile, db: JdbcProfile#API#Database)
    extends DBIOInterpreter[F] {
  protected implicit def F: Async[F]

  override val eval: DBIO ~> F = λ[DBIO ~> F](dbio => AsyncUtils.fromFuture(F.delay(db.run(dbio))))
}
