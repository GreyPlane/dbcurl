package io.github.greyplane.dbcrul.infrastructure

import cats.effect.kernel.Ref
import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import io.github.greyplane.dbcrul.infrastructure.Doobie._
import cats.effect.std.Console
import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
import doobie.Transactor

import java.util.concurrent.Executors
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

  def initTransactor(config: DBConfig): IO[HikariTransactor[IO]] = {
    // TODO shutdown executor
    val executor = Executors.newFixedThreadPool(4)
    val ec = ExecutionContext.fromExecutor(executor)
//    executor.shutdown()

    for {
      _ <- IO.delay(Class.forName(config.driver))
      t <- IO
        .delay(new HikariDataSource())
        .map(Transactor.fromDataSource[IO](_, ec, None))
      _ <- t.configure(ds =>
        IO.delay {
          ds setJdbcUrl config.url
          ds setUsername config.username
          ds setPassword config.password.value
        }
      )
    } yield t
  }

}
