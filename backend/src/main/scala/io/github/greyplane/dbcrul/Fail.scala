package io.github.greyplane.dbcrul

/** Base class for all failures in the application. The failures are translated to HTTP API results in the
  * [[io.github.greyplane.dbcrul.http.Http]] class.
  *
  * The class hierarchy is not sealed and can be extended as required by specific functionalities.
  */
abstract class Fail extends Exception

object Fail {
  case class NotFound(what: String) extends Fail
  case class Conflict(msg: String) extends Fail
  case class IncorrectInput(msg: String) extends Fail
  case class Unauthorized(msg: String) extends Fail
  case object Forbidden extends Fail
  case class Unsupported(what: String) extends Fail
}
