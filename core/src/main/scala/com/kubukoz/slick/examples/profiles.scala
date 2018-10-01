package com.kubukoz.slick.examples

import cats.implicits._
import cats.effect._
import com.kubukoz.slick.algebra.StreamingSelectAlgebra
import com.kubukoz.slick.interpreter.StreamingDBIOInterpreter
import fs2.Stream
import slick.basic.DatabasePublisher
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

  def program[F[_]: StreamingSelectAlgebra]: Stream[F, Int] = {
    StreamingSelectAlgebra[F].stream(
      TableQuery[Users].map(_.age)
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val interpreter: StreamingDBIOInterpreter[IO] =
      StreamingDBIOInterpreter.streamingInterpreter[IO](profiles.profile, db)

    program[IO].evalMap(age => IO(println(s"found age: $age"))).compile.drain
  }.as(ExitCode.Success)
}
