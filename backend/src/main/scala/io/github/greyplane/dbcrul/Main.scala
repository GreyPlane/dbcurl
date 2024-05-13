package io.github.greyplane.dbcrul

import cats.data.NonEmptyList
import cats.effect.{IO, Resource, ResourceApp}
import io.github.greyplane.dbcrul.config.{Config, Sensitive}
import io.github.greyplane.dbcrul.http.{Http, HttpApi}
import io.github.greyplane.dbcrul.infrastructure.{DB, DBConfig, Doobie}
import sttp.tapir.{endpoint, stringBody}
import db._
import cats.effect.std.Console

object Main extends ResourceApp.Forever {

  val config: Config = Config.read
  Config.log(config)

  val testConfig = DBConfig.MySQL("root", Sensitive(""), "jdbc:mysql://localhost:3306")

  val api = new HttpApi(
    new Http(),
    NonEmptyList(endpoint.in("echo").get.out(stringBody).serverLogicPure[IO](_ => Right("x")), Nil),
    config.api
  )

  val test = {
    DB.transactor(testConfig)
      .use(xa => {
        testConfig.getSchemas(xa).flatTap(schemas => Console[IO].println(schemas.mkString("\n")))
      })
  }

  /** Creating a resource which combines three resources in sequence:
    *
    *   - the first creates the object graph and allocates the dependencies
    *   - the second starts the background processes (here, an email sender)
    *   - the third allocates the http api resource
    *
    * Thanks to ResourceApp.Forever the result of the allocation is used by a non-terminating process (so that the http server is available
    * as long as our application runs).
    */
  override def run(list: List[String]): Resource[IO, Unit] = for {
    _ <- test.toResource
    _ <- api.resource
  } yield ()
}
