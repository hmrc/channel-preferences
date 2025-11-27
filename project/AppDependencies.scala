import sbt.*

object AppDependencies {

  val bootstrapVersion = "10.2.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % "2.13.0",
    "uk.gov.hmrc"        %% "domain-play-30"             % "13.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatest"          %% "scalatest"              % "3.2.19"         % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.2"          % Test,
    "org.scalatestplus"      %% "scalacheck-1-15"        % "3.2.11.0"       % Test
  )

  val itDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )
}
