import play.sbt.PlayImport.PlayKeys
import sbt.Resolver
import play.sbt.routes.RoutesKeys

val appName = "channel-preferences"

Global / majorVersion := 1
Global / scalaVersion := "3.3.3"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    RoutesKeys.routesImport += "uk.gov.hmrc.channelpreferences.ChannelBinder._",
    RoutesKeys.routesImport += "uk.gov.hmrc.channelpreferences.model.cds._",
    RoutesKeys.routesImport += "uk.gov.hmrc.channelpreferences.model.preferences._",
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(
    resolvers += Resolver.jcenterRepo
  )
  .settings(ScoverageSettings())

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.itDependencies
  )

scalafmtOnCompile := true
PlayKeys.playDefaultPort := 9052

dependencyUpdatesFailBuild := false
(Compile / compile) := ((Compile / compile) dependsOn dependencyUpdates).value
dependencyUpdatesFilter -= moduleFilter(organization = "org.scala-lang")

Compile / doc / sources := Seq.empty
