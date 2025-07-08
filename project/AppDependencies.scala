import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapVersion = "9.14.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatest"                %% "scalatest"              % "3.2.19"         % Test,
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "7.0.2"          % Test,
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0"       % Test
  )

  val itDependencies = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )
}
