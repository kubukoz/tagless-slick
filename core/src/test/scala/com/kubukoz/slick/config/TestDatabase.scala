package com.kubukoz.slick.config

import cats.effect.{Resource, Sync}
import com.kubukoz.slick.config.Profiles.SlickProfile.api._

object TestDatabase {

  def resource[F[_]: Sync]: Resource[F, Database] = {
    val unsafeDb: F[Database] = Sync[F].delay {
      Database.forURL("jdbc:postgresql://localhost/postgres", "postgres", "", driver = "org.postgresql.Driver")
    }

    Resource.fromAutoCloseable(unsafeDb)
  }
}
