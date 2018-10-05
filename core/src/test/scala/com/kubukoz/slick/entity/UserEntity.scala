package com.kubukoz.slick.entity

import com.kubukoz.slick.config.Profiles.SlickProfile.api._
import slick.lifted.ProvenShape

case class UserEntity(name: String, age: Int)

class Users(tag: Tag) extends Table[UserEntity](tag, "users") {

  val name: Rep[String] = column("name")
  val age: Rep[Int]     = column("age")

  override def * : ProvenShape[UserEntity] = (name, age) <> (UserEntity.tupled, UserEntity.unapply)
}
