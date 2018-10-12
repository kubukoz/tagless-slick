package com.kubukoz.slick
import cats.effect.IO
import cats.~>
import com.kubukoz.slick.config.Profiles.SlickProfile.api._
import com.kubukoz.slick.config.TestDatabase
import com.kubukoz.slick.interpreter.DBIOInterpreter
import com.kubukoz.slick.util.CatsEffectSpec
import org.scalatest.{AsyncFlatSpec, Matchers}
import slick.dbio.DBIO
import cats.implicits._

class DBIOInterpreterSpec extends AsyncFlatSpec with Matchers with CatsEffectSpec {

  "withPreparation" should "be applied before interpreting the DBIO" in {
    val program = TestDatabase.resource[IO].use { db =>
      val interpreter: DBIOInterpreter[IO] =
        DBIOInterpreter.interpreter[IO](slickProfile, db).withPreparation {
          new (DBIO ~> DBIO) {
            override def apply[A](fa: DBIO[A]): DBIO[A] = fa.transactionally
          }
        }

      val countUsers = interpreter.interpret(sql"select count(*) from users".as[Int].head)

      val dbio1 = sql"insert into users values('foo', 0)".asUpdate >> DBIO.failed(new Throwable("oops"))

      for {
        countBefore <- countUsers
        _           <- interpreter.interpret(dbio1).attempt.ensure(new Throwable("Transaction was expected to fail"))(_.isLeft)
        countAfter  <- countUsers
      } yield countAfter shouldBe countBefore
    }

    asyncTestTimed(program)
  }
}
