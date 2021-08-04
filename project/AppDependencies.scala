import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.7.0",
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-28",
    "uk.gov.hmrc"             %% "emailaddress"               % "3.5.0",
    "com.typesafe.play"       %% "play-json-joda"             % "2.7.3",
    "org.typelevel"           %% "cats-core"                  % "2.6.1"
  )

  // The fork of swagger-play requires a version of jackson-databind version >= 2.9.0 and < 2.10.0
  // Other libraries pulling in later jackson-databind include http-verbs and logback-json-logger
  val dependencyOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.9"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % "5.7.0"         % Test,
    "com.typesafe.play"      %% "play-test"                % current          % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"         % "test, it",
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.9.0"       % "test, it",
    "uk.gov.hmrc"            %% "service-integration-test" % "1.1.0-play-28" % "test, it",
    "org.pegdown"            % "pegdown"                   % "1.6.0"          % "test, it",
    "org.mockito"            % "mockito-core"              % "3.12.4"         % "test",
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.30.1"         % "test,it",
    "org.mock-server"        % "mockserver-netty"          % "5.11.2"         % "it",
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.36.8"         % "test, it"
  )
}
