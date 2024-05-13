package io.github.greyplane.dbcrul.db

import cats.effect.IO
import doobie.util.transactor.Transactor

trait HasSchema[Db] {

  def getSchemas(xa: Transactor[IO]): IO[List[Schema]]

}
