import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.8.0",
    "com.typesafe.play" %% "play-json-joda"            % "2.9.3",
    "org.typelevel"     %% "cats-core"                 % "2.8.0",
  )

  // Several dependencies require a lower major version of Scala 2.13 xml library
  val dependencyOverrides: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % "7.8.0"         % Test,
    "com.typesafe.play"      %% "play-test"                % "2.8.18"        % Test,
    "org.scalactic"          %% "scalactic"                % "3.2.14"        % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.14"        % "test, it",
    "org.scalacheck"         %% "scalacheck"               % "1.17.0"        % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"         % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.11.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.10.0"      % "test, it",
    "uk.gov.hmrc"            %% "service-integration-test" % "1.3.0-play-28" % "test, it",
    "org.mockito"            %% "mockito-scala"            % "1.17.12"       % "test, it",
    "com.github.tomakehurst" % "wiremock"                  % "2.33.2"        % "test, it",
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.64.0"        % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1"      % "test, it",
  )
}
