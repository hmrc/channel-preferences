import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapVersion = "9.13.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % "2.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatest"                %% "scalatest"              % "3.2.17"         % Test,
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "7.0.1"          % Test,
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0"       % Test,
    "org.scalatestplus"            %% "mockito-4-11"           % "3.2.17.0"       % Test,
    "com.github.tomakehurst"        % "wiremock"               % "2.33.2"         % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.16.1"         % Test
  )

  val itDependencies = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )
}
