package io.github.greyplane.dbcrul.db.capability

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.github.greyplane.dbcrul.db.model.DbSchema
import io.github.greyplane.dbcrul.infrastructure.DBConfig

trait HasSchemaSyntax {

  implicit class EnrichDb[Db <: DBConfig](db: Db) {
    def getSchemas(xa: Transactor[IO])(implicit hasSchema: HasSchema[Db]): IO[List[DbSchema]] = {
      hasSchema.getSchemas(xa)
    }
  }

}
