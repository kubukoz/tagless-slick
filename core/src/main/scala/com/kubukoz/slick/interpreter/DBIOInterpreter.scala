package com.kubukoz.slick.interpreter

import cats.arrow.FunctionK
import cats.effect.{Async, Concurrent}
import cats.tagless.finalAlg
import cats.~>
import com.kubukoz.slick.interpreter.DBIOInterpreter.WithApi
import com.kubukoz.slick.interpreter.internal.AsyncUtils
import slick.dbio._
import slick.jdbc.JdbcProfile

@finalAlg
trait DBIOInterpreter[F[_]] { self =>
  protected def profile: JdbcProfile

  def interpret: DBIO ~> F

  private[slick] final def withApi: WithApi = new WithApi(profile)

  def withPreparation(preInterpret: DBIO ~> DBIO): DBIOInterpreter[F]
}

object DBIOInterpreter {

  def interpreter[F[_]: Concurrent](profile: JdbcProfile, db: JdbcProfile#API#Database): DBIOInterpreter[F] = {
    new AsyncInterpreter[F](profile, db, FunctionK.id) {
      override protected val F: Async[F] = Async[F]
    }
  }

  final class WithApi private[DBIOInterpreter] (val profile: JdbcProfile) extends AnyVal {
    def apply[A](f: profile.API => A): A = f(profile.api)
  }
}

private[interpreter] abstract class AsyncInterpreter[F[_]](val profile: JdbcProfile,
                                                           db: JdbcProfile#API#Database,
                                                           rawTrans: DBIO ~> DBIO)
    extends DBIOInterpreter[F] { self =>
  protected implicit def F: Async[F]

  private def runDBIO[A](dbio: DBIO[A]): F[A] = AsyncUtils.fromFuture(F.delay(db.run(dbio)))

  override val interpret: DBIO ~> F = rawTrans.andThen(λ[DBIO ~> F](runDBIO(_)))

  override def withPreparation(preInterpret: ~>[DBIO, DBIO]): DBIOInterpreter[F] =
    new AsyncInterpreter[F](profile, db, preInterpret.andThen(rawTrans)) {
      override protected implicit def F: Async[F] = self.F
    }
}
