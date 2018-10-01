package com.kubukoz.slick.examples

import cats.effect._
import cats.implicits._
import com.kubukoz.slick.algebra.StreamingSelectAlgebra
import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import fs2.Stream
import fs2.Stream.Compiler
import slick.jdbc.{JdbcProfile, PostgresProfile}
import slick.lifted.{ProvenShape, TableQuery}

object profiles {
  val profile: JdbcProfile = PostgresProfile
}

import com.kubukoz.slick.examples.profiles.profile.api._

case class UserEntity(name: String, age: Int)

class Users(tag: Tag) extends Table[UserEntity](tag, "users") {

  val name: Rep[String] = column("name")
  val age: Rep[Int]     = column("age")

  override def * : ProvenShape[UserEntity] = (name, age) <> (UserEntity.tupled, UserEntity.unapply)
}

object Tests extends IOApp {

  val db: Database =
    Database.forURL("jdbc:postgresql://localhost/postgres", "postgres", "example", driver = "org.postgresql.Driver")

  def program[F[_]: StreamingSelectAlgebra: Console, G[_]](implicit streamCompiler: Compiler[F, G]): G[Unit] = {
    StreamingSelectAlgebra[F]
      .stream(TableQuery[Users].map(_.age))
      .evalMap(age => Console[F].putStrLn(s"found age: $age"))
      .compile
      .drain
  }

  def abstractApplicationProxyBeanFactoryDelegateProviderRepositoryInjectionStrategyVisitor[F[_]: ConcurrentEffect]
    : F[Unit] = {

    implicit val interpreter: StreamingDBIOInterpreter[F] =
      StreamingDBIOInterpreter.streamingInterpreter[F](profiles.profile, db)

    implicit val console: Console[F] = new SyncConsole[F]

    program[F, F]
  }

  override def run(args: List[String]): IO[ExitCode] =
    abstractApplicationProxyBeanFactoryDelegateProviderRepositoryInjectionStrategyVisitor[IO].as(ExitCode.Success)
}
