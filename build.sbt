import sbtbuildinfo.BuildInfoKey.action
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}
import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings

import sbt._
import Keys._

import scala.util.Try
import scala.sys.process.Process
import complete.DefaultParsers._

val doobieVersion = "1.0.0-RC5"
val http4sVersion = "0.23.26"
val http4sBlazeVersion = "0.23.16"
val circeVersion = "0.14.6"
val tapirVersion = "1.10.3"
val macwireVersion = "2.5.9"

val dbDependencies = Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "com.mysql" % "mysql-connector-j" % "8.3.0"
)

val httpDependencies = Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sBlazeVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-files" % tapirVersion
)

val jsonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
)

val configDependencies = Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.17.6"
)

val baseDependencies = Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "co.fs2" %% "fs2-core" % "3.10.2",
  "com.softwaremill.common" %% "tagging" % "2.3.4",
  "com.softwaremill.quicklens" %% "quicklens" % "1.9.7"
)

val scalatest = "org.scalatest" %% "scalatest" % "3.2.18" % Test
val macwireDependencies = Seq(
  "com.softwaremill.macwire" %% "macrosautocats" % macwireVersion
).map(_ % Provided)

val unitTestingStack = Seq(scalatest)

val embeddedPostgres = "com.opentable.components" % "otj-pg-embedded" % "1.0.2" % Test
val dbTestingStack = Seq(embeddedPostgres)

val commonDependencies = baseDependencies ++ unitTestingStack ++ configDependencies

lazy val uiProjectName = "ui"
lazy val uiDirectory = settingKey[File]("Path to the ui project directory")
lazy val npmTask = inputKey[Unit]("Run npm with arguments")
lazy val copyWebapp = taskKey[Unit]("Copy webapp")

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "io.github.greyplane.dbcrul",
  scalaVersion := "2.13.12",
  libraryDependencies ++= commonDependencies,
  uiDirectory := (ThisBuild / baseDirectory).value / uiProjectName,
  npmTask := {
    val taskName = spaceDelimited("<arg>").parsed.mkString(" ")
    val localYarnCommand = "npm run " + taskName
    def runYarnTask() = Process(localYarnCommand, uiDirectory.value).!
    streams.value.log("Running npm task: " + taskName)
    haltOnCmdResultError(runYarnTask())
  }
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    action("lastCommitHash") {
      import scala.sys.process._
      // if the build is done outside of a git repository, we still want it to succeed
      Try("git rev-parse HEAD".!!.trim).getOrElse("?")
    }
  ),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.ToMap,
  buildInfoPackage := "io.github.greyplane.dbcrul.version",
  buildInfoObject := "BuildInfo"
)

lazy val fatJarSettings = Seq(
  assembly / assemblyJarName := "dbcrul.jar",
  assembly := assembly.dependsOn(copyWebapp).value,
  assembly / assemblyMergeStrategy := {
    case PathList(ps @ _*) if ps.last endsWith "io.netty.versions.properties"       => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "pom.properties"                     => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "scala-collection-compat.properties" => MergeStrategy.first
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val dockerSettings = Seq(
  dockerExposedPorts := Seq(8080),
  dockerBaseImage := "adoptopenjdk:11.0.5_10-jdk-hotspot",
  Docker / packageName := "dbcrul",
  dockerUsername := Some("softwaremill"),
  dockerUpdateLatest := true,
  Docker / publishLocal := (Docker / publishLocal).dependsOn(copyWebapp).value,
  Docker / version := git.gitDescribedVersion.value.getOrElse(git.formattedShaVersion.value.getOrElse("latest")),
  git.uncommittedSignifier := Some("dirty"),
  ThisBuild / git.formattedShaVersion := {
    val base = git.baseVersion.?.value
    val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
    git.gitHeadCommit.value.map { sha =>
      git.defaultFormatShaVersion(base, sha.take(7), suffix)
    }
  }
)

def haltOnCmdResultError(result: Int) {
  if (result != 0) {
    throw new Exception("Build failed.")
  }
}

def now(): String = {
  import java.text.SimpleDateFormat
  import java.util.Date
  new SimpleDateFormat("yyyy-MM-dd-hhmmss").format(new Date())
}

lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "dbcrul",
    Compile / herokuFatJar := Some((backend / assembly / assemblyOutputPath).value),
    Compile / deployHeroku := ((Compile / deployHeroku) dependsOn (backend / assembly)).value
  )
  .aggregate(backend, ui)

lazy val backend: Project = (project in file("backend"))
  .settings(
    libraryDependencies ++= dbDependencies ++ httpDependencies ++ jsonDependencies ++ macwireDependencies,
    Compile / mainClass := Some("io.github.greyplane.dbcrul.Main"),
    copyWebapp := {
      val source = uiDirectory.value / "build"
      val target = (Compile / classDirectory).value / "webapp"
      streams.value.log.info(s"Copying the webapp resources from $source to $target")
      IO.copyDirectory(source, target)
    },
    copyWebapp := copyWebapp.dependsOn(npmTask.toTask(" build")).value
  )
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(commonSettings)
  .settings(Revolver.settings)
  .settings(buildInfoSettings)
  .settings(
    graalVMNativeImageOptions ++= List(
      "-H:IncludeResources=.+\\.conf",
      "-H:IncludeResources=(.*/)*(.*.css)|(.*.html)|(.*.js)|(.*.json)|(.*.png)$",
      "--verbose"
    )
  )
//  .settings(fatJarSettings)
//  .enablePlugins(DockerPlugin)
//  .enablePlugins(JavaServerAppPackaging)
//  .settings(dockerSettings)

lazy val ui = (project in file(uiProjectName))
  .settings(commonSettings)
  .settings(Test / test := (Test / test).dependsOn(npmTask.toTask(" test:ci")).value)
  .settings(cleanFiles += baseDirectory.value / "build")

RenameProject.settings
