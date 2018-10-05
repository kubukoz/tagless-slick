package com.kubukoz.slick.util

import cats.effect._
import cats.effect.implicits._
import org.scalatest.Assertion

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait CatsEffectSpec {
  implicit protected val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
  implicit protected val timer: Timer[IO]     = IO.timer(ExecutionContext.Implicits.global)

  protected val timeout: FiniteDuration = 5.seconds

  final def asyncTestTimed[F[_]: ConcurrentEffect: Timer](test: F[Assertion]): Future[Assertion] = {
    test.timeout(timeout).toIO.unsafeToFuture()
  }
}
