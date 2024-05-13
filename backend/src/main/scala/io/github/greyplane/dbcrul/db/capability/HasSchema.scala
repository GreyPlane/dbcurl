package io.github.greyplane.dbcrul.db.capability

import cats.effect.IO
import doobie.util.transactor.Transactor
import io.github.greyplane.dbcrul.db.model.DbSchema

trait HasSchema[Db] {

  def getSchemas(xa: Transactor[IO]): IO[List[DbSchema]]

}
