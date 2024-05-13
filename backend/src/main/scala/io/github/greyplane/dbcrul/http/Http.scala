package io.github.greyplane.dbcrul.http

import cats.effect.IO
import cats.implicits._
import io.circe.Printer
import io.github.greyplane.dbcrul._
import io.github.greyplane.dbcrul.infrastructure.Json._
import sttp.model.StatusCode
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.{EndpointOutput, PublicEndpoint, Tapir}

/** Helper class for defining HTTP endpoints. Import the members of this class when defining an HTTP API using tapir. */
object Http extends Tapir with TapirJsonCirce with TapirSchemas {

  val jsonErrorOutOutput: EndpointOutput[Error_OUT] = jsonBody[Error_OUT]

  /** Description of the output, that is used to represent an error that occurred during endpoint invocation. */
  val failOutput: EndpointOutput[(StatusCode, Error_OUT)] = statusCode.and(jsonErrorOutOutput)

  /** Base endpoint description for non-secured endpoints. Specifies that errors are always returned as JSON values corresponding to the
    * [[Error_OUT]] class.
    */
  val baseEndpoint: PublicEndpoint[Unit, (StatusCode, Error_OUT), Unit, Any] =
    endpoint
      .errorOut(failOutput)
      // Prevent clickjacking attacks: https://cheatsheetseries.owasp.org/cheatsheets/Clickjacking_Defense_Cheat_Sheet.html
      .out(header("X-Frame-Options", "DENY"))
      .out(header("Content-Security-Policy", "frame-ancestors 'none'"))

  private val InternalServerError = (StatusCode.InternalServerError, "Internal server error")
  private val failToResponseData: Fail => (StatusCode, String) = {
    case Fail.NotFound(what)      => (StatusCode.NotFound, what)
    case Fail.Conflict(msg)       => (StatusCode.Conflict, msg)
    case Fail.IncorrectInput(msg) => (StatusCode.BadRequest, msg)
    case Fail.Forbidden           => (StatusCode.Forbidden, "Forbidden")
    case Fail.Unauthorized(msg)   => (StatusCode.Unauthorized, msg)
    case Fail.Unsupported(what)   => (StatusCode.BadRequest, s"Unsupported operation: $what")
    case _                        => InternalServerError
  }

  //

  implicit class IOOut[T](f: IO[T]) {

    /** An extension method for [[IO]], which converts a possibly failed IO, to one which either returns the error converted to an
      * [[Error_OUT]] instance, or returns the successful value unchanged.
      */
    def toOut: IO[Either[(StatusCode, Error_OUT), T]] = {
      f.map(t => t.asRight[(StatusCode, Error_OUT)]).recoverWith { case f: Fail =>
        val (statusCode, message) = failToResponseData(f)
        IO.pure((statusCode, Error_OUT(message)).asLeft[T])
      }
    }
  }

  override def jsonPrinter: Printer = noNullsPrinter
}

/** Schemas for types used in endpoint descriptions (as parts of query parameters, JSON bodies, etc.). Includes explicitly defined schemas
  * for custom types, and auto-derivation for ADTs & value classes.
  */
trait TapirSchemas extends SchemaDerivation

case class Error_OUT(error: String)
