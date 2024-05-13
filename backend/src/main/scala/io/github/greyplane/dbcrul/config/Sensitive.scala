package io.github.greyplane.dbcrul.config

import io.circe.Decoder

case class Sensitive(value: String) extends AnyVal {
  override def toString: String = "***"
}

object Sensitive {
  implicit val decoder = Decoder[String].map(Sensitive.apply)
}
