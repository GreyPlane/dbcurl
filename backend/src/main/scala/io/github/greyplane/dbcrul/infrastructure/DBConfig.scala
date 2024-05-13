package io.github.greyplane.dbcrul.infrastructure

import cats.effect.IO
import doobie.util.transactor
import io.github.greyplane.dbcrul.config.Sensitive
import io.github.greyplane.dbcrul.db.{HasSchema, Schema}
import Doobie._

sealed trait DBConfig {
  def username: String
  def password: Sensitive
  def url: String
  def driver: String
}

object DBConfig {
  case class MySQL(username: String, password: Sensitive, url: String) extends DBConfig {
    override def driver: String = "com.mysql.cj.jdbc.Driver"
  }

  object MySQL {

    private val schemaSQL = sql"select schema_name as name from information_schema.schemata order by name"

    implicit val hasSchema: HasSchema[MySQL] = new HasSchema[MySQL] {
      def getSchemas(xa: transactor.Transactor[IO]): IO[List[Schema]] = {
        schemaSQL.query[Schema].to[List].transact(xa)
      }
    }
  }

  case class PostgreSQL(username: String, password: Sensitive, url: String) extends DBConfig {
    override def driver: String = "org.postgresql.Driver"
  }
}
