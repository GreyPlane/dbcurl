package io.github.greyplane.dbcrul.http

import cats.data.NonEmptyList
import cats.effect.std.Console
import cats.effect.{IO, Resource}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir._
import sttp.tapir.files.{FilesOptions, staticResourcesGetServerEndpoint}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

/** Interprets the endpoint descriptions (defined using tapir) as http4s routes, adding CORS, metrics, api docs support.
  *
  * The following endpoints are exposed:
  *   - `/api/v1` - the main API
  *   - `/api/v1/docs` - swagger UI for the main API
  *   - `/admin` - admin API
  *   - `/` - serving frontend resources
  */
class HttpApi(
    mainEndpoints: NonEmptyList[ServerEndpoint[Any, IO]],
    config: HttpConfig
) {
  private val apiContextPath = List("api", "v1")

  val serverOptions: Http4sServerOptions[IO] = Http4sServerOptions
    .customiseInterceptors[IO]
    // all errors are formatted as json, and there are no other additional http4s routes
    .defaultHandlers(msg => ValuedEndpointOutput(Http.jsonErrorOutOutput, Error_OUT(msg)), notFoundWhenRejected = true)
    .serverLog {
      DefaultServerLog(
        doLogWhenReceived = request => Console[IO].println(request),
        doLogWhenHandled = (request, maybeError) => Console[IO].println(request),
        doLogAllDecodeFailures = (request, maybeFailure) => Console[IO].println(s"$request decode failed, bcs $maybeFailure"),
        doLogExceptions = (msg: String, ex: Throwable) => Console[IO].println(msg) >> Console[IO].printStackTrace(ex),
        noLog = IO.pure(())
      )
    }
    .corsInterceptor(CORSInterceptor.default[IO])
    .options

  lazy val routes: HttpRoutes[IO] = Http4sServerInterpreter(serverOptions).toRoutes(allEndpoints)

  lazy val docEnpdoints =
    SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.contextPath(apiContextPath))
      .fromServerEndpoints(mainEndpoints.toList, "dbcurl", "v1")

  lazy val allEndpoints: List[ServerEndpoint[Any, IO]] = {
    // creating the documentation using `mainEndpoints` without the /api/v1 context path; instead, a server will be added
    // with the appropriate suffix
    // for /api/v1 requests, first trying the API; then the docs
    val apiEndpoints =
      mainEndpoints.map(se => se.prependSecurityIn(apiContextPath.foldLeft(emptyInput: EndpointInput[Unit])(_ / _))) ++ docEnpdoints

    // for all other requests, first trying getting existing webapp resource (html, js, css files), from the /webapp
    // directory on the classpath; otherwise, returning index.html; this is needed to support paths in the frontend
    // apps (e.g. /login) the frontend app will handle displaying appropriate error messages
    val webappEndpoints = List(
      staticResourcesGetServerEndpoint[IO](emptyInput: EndpointInput[Unit])(
        classOf[HttpApi].getClassLoader,
        "webapp",
        FilesOptions.default[IO].defaultFile(List("index.html"))
      )
    )
    apiEndpoints.toList ++ webappEndpoints
  }

  /** The resource describing the HTTP server; binds when the resource is allocated. */
  lazy val resource: Resource[IO, org.http4s.server.Server] = BlazeServerBuilder[IO]
    .bindLocal(config.port)
    .withHttpApp(routes.orNotFound)
    .resource
}
