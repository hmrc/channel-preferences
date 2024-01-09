import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel"     %% "cats-core"                 % "2.10.0",
  )

  // Several dependencies require a lower major version of Scala 2.13 xml library
//  val dependencyOverrides: Seq[ModuleID] = Seq(
//    "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
//  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"    % bootstrapVersion % Test,
    "com.typesafe.play"            %% "play-test"                 % "2.9.1"          % Test,
    "org.scalactic"                %% "scalactic"                 % "3.2.17"         % Test,
    "org.scalatest"                %% "scalatest"                 % "3.2.17"         % "test, it",
    "org.scalacheck"               %% "scalacheck"                % "1.17.0"         % Test,
    "org.scalatestplus.play"       %% "scalatestplus-play"        % "7.0.1"          % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"           % "3.2.11.0"       % "test, it",
    "org.scalatestplus"            %% "mockito-3-4"               % "3.2.10.0"       % "test, it",
    "org.mockito"                  %% "mockito-scala"             % "1.17.30"        % "test, it",
    "com.github.tomakehurst"       %  "wiremock"                  % "3.0.1"          % "test, it",
    "com.vladsch.flexmark"         %  "flexmark-all"              % "0.64.8"         % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.16.1"         % "test, it",
  )

  val itDependencies = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"    % bootstrapVersion % Test
  )
}
