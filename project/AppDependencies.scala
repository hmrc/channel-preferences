import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.7.0",
    //"uk.gov.hmrc"       %  "emailaddress_2.13"         % "3.6.0",
    "com.typesafe.play" %% "play-json-joda"            % "2.9.3",
    "org.typelevel"     %% "cats-core"                 % "2.8.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4"
  )

  // The fork of swagger-play requires a version of jackson-databind version >= 2.9.0 and < 2.10.0
  // Other libraries pulling in later jackson-databind include http-verbs and logback-json-logger
  val dependencyOverrides: Seq[ModuleID] = Seq()
//    "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4"
//  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % "7.7.0"         % Test,
    "com.typesafe.play"      %% "play-test"                % current         % Test,
    "org.scalactic"          %% "scalactic"                % "3.2.14"        % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.14"        % "test, it",
    "org.scalacheck"         %% "scalacheck"               % "1.17.0"        % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"         % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.11.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.10.0"      % "test, it",
    "uk.gov.hmrc"            %% "service-integration-test" % "1.3.0-play-28" % "test, it",
//    "org.pegdown"            % "pegdown"                   % "1.6.0"         % "test, it",
    "org.mockito"            %% "mockito-scala"            % "1.17.12"       % "test, it",
//    "org.mockito"            % "mockito-core"              % "4.8.0"         % "test",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.12"       % Test,
    "com.github.tomakehurst" % "wiremock"                  % "2.33.2"        % "test, it",
    "org.mock-server"        % "mockserver-netty"          % "5.14.0"        % "it",
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.64.0"        % "test, it",
    "com.vladsch.flexmark"   % "flexmark-util"             % "0.64.0"        % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4"      % "test, it",
    "com.google.inject"      % "guice"                     % "5.1.0"         % "test, it"
  )
}
