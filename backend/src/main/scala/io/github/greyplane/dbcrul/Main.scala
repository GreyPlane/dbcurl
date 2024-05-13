package io.github.greyplane.dbcrul

import cats.data.NonEmptyList
import cats.effect.std.{Console, MapRef}
import cats.effect.{IO, Resource, ResourceApp}
import doobie.hikari.HikariTransactor
import io.github.greyplane.dbcrul.config.{Config, Sensitive}
import io.github.greyplane.dbcrul.db._
import io.github.greyplane.dbcrul.http.HttpApi
import io.github.greyplane.dbcrul.infrastructure.{DB, DBConfig}
import sttp.tapir.{endpoint, stringBody}

object Main extends ResourceApp.Forever {

  val config: Config = Config.read
  Config.log(config)

  val testConfig = DBConfig.MySQL("root", Sensitive(""), "jdbc:mysql://localhost:3306")

  val api = new HttpApi(
    NonEmptyList(endpoint.in("echo").get.out(stringBody).serverLogicPure[IO](_ => Right("x")), Nil),
    config.api
  )

//  val test = {
//    DB.transactor(testConfig)
//      .use(xa => {
//        testConfig.getSchemas(xa).flatTap(schemas => Console[IO].println(schemas.mkString("\n")))
//      })
//  }

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
    dbs <- MapRef.ofScalaConcurrentTrieMap[IO, String, DB].toResource
    dbApi = new db.Api(dbs)
    httpApi = new HttpApi(dbApi.dbEndpoints, config.api)
    _ <- httpApi.resource
  } yield ()
}
