package io.github.greyplane.dbcrul.infrastructure

import io.circe.Printer
import io.circe.generic.AutoDerivation

/** Import the members of this object when doing JSON serialisation or deserialisation.
  */
object Json extends AutoDerivation {
  val noNullsPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
}
