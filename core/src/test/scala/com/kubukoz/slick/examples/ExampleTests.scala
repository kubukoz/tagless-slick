package com.kubukoz.slick.examples

import cats.arrow.FunctionK
import cats.{~>, Applicative, Show}
import cats.effect._
import cats.implicits._
import cats.effect.concurrent.Ref
import com.kubukoz.slick.algebra.StreamingSelectAlgebra
import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import fs2.Stream
import fs2.Stream.Compiler
import org.scalatest.{AsyncFlatSpec, Matchers}
import slick.jdbc.{JdbcProfile, PostgresProfile}
import slick.lifted.ProvenShape

class ExampleTests extends AsyncFlatSpec with Matchers {

  object internalss {

    object profiles {
      val profile: JdbcProfile = PostgresProfile
    }

    import profiles.profile.api._

    case class UserEntity(name: String, age: Int)

    class Users(tag: Tag) extends Table[UserEntity](tag, "users") {

      val name: Rep[String] = column("name")
      val age: Rep[Int]     = column("age")

      override def * : ProvenShape[UserEntity] = (name, age) <> (UserEntity.tupled, UserEntity.unapply)
    }

    val db: Database =
      Database.forURL("jdbc:postgresql://localhost/postgres", "postgres", "example", driver = "org.postgresql.Driver")
  }

  type StreamInts[F[_]] = StreamingSelectAlgebra[F, Int]
  def StreamInts[F[_]: StreamInts]: StreamInts[F] = implicitly

  import internalss.profiles.profile.api._

  def program[F[_]: StreamInts: Console, G[_]: Applicative](implicit streamCompiler: Compiler[F, G],
                                                            fToG: F ~> G): G[List[Int]] = {
    val q = TableQuery[internalss.Users].map(_.age)

    val printEach = StreamInts[F].stream(q).evalMap(age => Console[F].putStrLn(s"found age: $age")).compile.drain

    printEach *> fToG(StreamInts[F].all(q))
  }

  //for production
  def abstractApplicationProxyBeanFactoryDelegateProviderRepositoryInjectionStrategyVisitor[F[_]: ConcurrentEffect]
    : F[List[Int]] = {

    implicit val interpreter: StreamingDBIOInterpreter[F] =
      StreamingDBIOInterpreter.streamingInterpreter[F](internalss.profiles.profile, internalss.db)

    implicit val console: Console[F] = new SyncConsole[F]

    implicit val idK: F ~> F = FunctionK.id[F]

    program[F, F]
  }

  def testProgram[F[_]: Sync](ref: Ref[F, List[String]]): F[List[Int]] = {
    implicit val constStream: StreamInts[F] = new StreamingSelectAlgebra[F, Int] {
      override def stream[T](query: Query[T, Int, Seq]): Stream[F, Int] = Stream(1, 2, 3)
      override def all[T](query: Query[T, Int, Seq]): F[List[Int]]      = stream(query).compile.toList
    }

    implicit val console: Console[F] = new SyncConsole[F] {
      override def putStrLn[A](a: A)(implicit A: Show[A]): F[Unit] = ref.update(A.show(a) :: _)
    }

    implicit val idK: F ~> F = FunctionK.id[F]

    program[F, F]
  }

  "the program" should "work with stubbed dependencies" in {
    val test = for {
      ref          <- Ref[IO].of(List.empty[String])
      result       <- testProgram(ref)
      printedLines <- ref.get
    } yield {
      printedLines shouldBe List(
        "found age: 3",
        "found age: 2",
        "found age: 1"
      )

      result shouldBe List(1, 2, 3)
    }

    test.unsafeToFuture()
  }
}
