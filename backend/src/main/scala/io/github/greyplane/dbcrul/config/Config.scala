package io.github.greyplane.dbcrul.config

import io.github.greyplane.dbcrul.http.HttpConfig
import io.github.greyplane.dbcrul.version.BuildInfo
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.collection.immutable.TreeMap

/** Maps to the `application.conf` file. Configuration for all modules of the application. */
case class Config(api: HttpConfig)

object Config {
  def log(config: Config): Unit = {
    val baseInfo = s"""
                      |Dbcrul configuration:
                      |-----------------------
                      |API:            ${config.api}
                      |
                      |Build & env info:
                      |-----------------
                      |""".stripMargin

    val info = TreeMap(BuildInfo.toMap.toSeq: _*).foldLeft(baseInfo) { case (str, (k, v)) =>
      str + s"$k: $v\n"
    }

    println(info)
  }

  def read: Config = ConfigSource.default.loadOrThrow[Config]
}
