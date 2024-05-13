package io.github.greyplane.dbcrul.db

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.std.MapRef
import doobie.hikari.HikariTransactor
import io.github.greyplane.dbcrul.db.model.DbSchema
import io.github.greyplane.dbcrul.http.Http._
import io.github.greyplane.dbcrul.infrastructure.Json._
import io.github.greyplane.dbcrul.infrastructure.{DB, DBConfig}
import io.github.greyplane.dbcrul.Fail
import cats.implicits._
import sttp.tapir.EndpointInput

class Api(dbs: MapRef[IO, String, Option[(DBConfig, HikariTransactor[IO])]]) {

  import Api._

  private val addDb = baseEndpoint.post.in(path[String]).in(jsonBody[DBConfig]).out(stringBody).serverLogic[IO] { case (id, config) =>
    DB.initTransactor(config)
      .flatMap(xa => dbs.getAndSetKeyValue(id, config -> xa))
      .flatTap {
        case Some((_, previousXa)) =>
          IO.delay(previousXa.kernel.close())
        case None => IO.unit
      }
      .map(_ => id)
      .toOut
  }

  private val removeDb = baseEndpoint.delete
    .in(path[String])
    .out(stringBody)
    .serverLogic[IO](id =>
      dbs(id)
        .flatModify {
          case Some((config, xa)) => (None, IO.delay(xa.kernel.close()))
          case None               => (None, IO.unit)
        }
        .map(_ => id)
        .toOut
    )

  private val getSchemas = baseEndpoint.get
    .in(path[String])
    .in("schema")
    .out(jsonBody[List[DbSchema]])
    .serverLogic(id =>
      dbs(id).get.flatMap {
        case Some((config, xa)) =>
          config match {
            case db @ DBConfig.MySQL(username, password, url) => db.getSchemas(xa)
            case DBConfig.PostgreSQL(username, password, url) => Fail.Unsupported("Get schemas for PG").raiseError[IO, List[DbSchema]]
          }
        case None => Fail.NotFound(s"DB $id").raiseError[IO, List[DbSchema]]
      }.toOut
    )

  val dbEndpoints = NonEmptyList.of(addDb, removeDb, getSchemas).map(_.prependSecurityIn(dbOperationContextPath))
}

object Api {

  private val dbOperationContextPath = List("db").foldLeft(emptyInput: EndpointInput[Unit])(_ / _)

}
