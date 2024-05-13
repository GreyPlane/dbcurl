package io.github.greyplane.dbcrul.config

case class Sensitive(value: String) extends AnyVal {
  override def toString: String = "***"
}
