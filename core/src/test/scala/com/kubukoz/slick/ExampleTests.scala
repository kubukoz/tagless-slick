package com.kubukoz.slick

import cats.arrow.FunctionK
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.{~>, Applicative, Show}
import com.kubukoz.slick.algebra.StreamingSelectAlgebra
import com.kubukoz.slick.config.Profiles.SlickProfile
import com.kubukoz.slick.config.Profiles.SlickProfile.api._
import com.kubukoz.slick.config.TestDatabase
import com.kubukoz.slick.entity.Users
import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import com.kubukoz.slick.util.CatsEffectSpec
import fs2.Stream
import fs2.Stream.Compiler
import org.scalatest.{AsyncFlatSpec, Matchers}
import slick.lifted

class ExampleTests extends AsyncFlatSpec with Matchers with CatsEffectSpec {

  type StreamInts[F[_]] = StreamingSelectAlgebra[F, Int]
  def StreamInts[F[_]: StreamInts]: StreamInts[F] = implicitly

  def program[F[_]: StreamInts: Console, G[_]: Applicative](implicit streamCompiler: Compiler[F, G],
                                                            fToG: F ~> G): G[List[Int]] = {
    val q = lifted.TableQuery[Users].map(_.age)

    val printEach = StreamInts[F].stream(q).evalMap(age => Console[F].putStrLn(s"found age: $age")).compile.drain

    printEach *> fToG(StreamInts[F].all(q))
  }

  //for production
  def abstractApplicationProxyBeanFactoryDelegateProviderRepositoryInjectionStrategyVisitor[F[_]: ConcurrentEffect]
    : F[List[Int]] = TestDatabase.resource[F].use { db =>
    implicit val interpreter: StreamingDBIOInterpreter[F] =
      StreamingDBIOInterpreter.streamingInterpreter[F](SlickProfile, db)

    implicit val console: Console[F] = new SyncConsole[F]

    implicit val idK: F ~> F = FunctionK.id[F]

    program[F, F]
  }

  def testProgram[F[_]: Sync](ref: Ref[F, List[String]]): F[List[Int]] = {
    implicit val constStream: StreamInts[F] = new StreamingSelectAlgebra[F, Int] {
      override def stream(query: Query[_, Int, Seq]): Stream[F, Int] = Stream(1, 2, 3)
      override def all(query: Query[_, Int, Seq]): F[List[Int]]      = stream(query).compile.toList
    }

    implicit val console: Console[F] = new SyncConsole[F] {
      override def putStrLn[A](a: A)(implicit A: Show[A]): F[Unit] = ref.update(A.show(a) :: _)
    }

    implicit val idK: F ~> F = FunctionK.id[F]

    program[F, F]
  }

  "the program" should "work with stubbed dependencies" in asyncTestTimed {
    for {
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
  }

  "the real thing" should "do something too" in asyncTestTimed {
    for {
      result <- abstractApplicationProxyBeanFactoryDelegateProviderRepositoryInjectionStrategyVisitor[IO]
    } yield {
      result shouldBe List(22, 35)
    }
  }
}
