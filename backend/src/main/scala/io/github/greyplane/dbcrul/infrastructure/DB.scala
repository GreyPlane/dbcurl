package io.github.greyplane.dbcrul.infrastructure

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import io.github.greyplane.dbcrul.infrastructure.Doobie._
import cats.effect.std.Console
import cats.implicits._

import scala.concurrent.ExecutionContext

object DB {
  private def testConnection(xa: Transactor[IO]): IO[Unit] =
    IO {
      sql"select 1".query[Int].unique.transact(xa)
    }.void

  def transactor(config: DBConfig): Resource[IO, HikariTransactor[IO]] = {
    val ec = doobie.util.ExecutionContexts.fixedThreadPool[IO](1)

    def buildTransactor(ec: ExecutionContext) =
      HikariTransactor.newHikariTransactor[IO](config.driver, config.url, config.username, config.password.value, ec)

    ec.flatMap(buildTransactor).evalTap(xa => testConnection(xa) >> Console[IO].println("db connected"))
  }
}
