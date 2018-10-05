package com.kubukoz.slick.config
import slick.jdbc.PostgresProfile

object Profiles {
  type SlickProfile = PostgresProfile
  val SlickProfile: SlickProfile = PostgresProfile
}
