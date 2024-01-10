import play.sbt.PlayImport.PlayKeys
import uk.gov.hmrc.DefaultBuildSettings
//import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.*
import sbt.Resolver
import play.sbt.routes.RoutesKeys

val appName = "channel-preferences"

ThisProject / majorVersion := 0
ThisProject / scalaVersion := "2.13.12"
ThisBuild / scalaVersion := "2.13.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SwaggerPlugin)
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
    majorVersion := 0,
    scalaVersion := "2.13.12",
    libraryDependencies ++= AppDependencies.itDependencies,
    DefaultBuildSettings.integrationTestSettings()
  )

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := (Compile / scalastyle).toTask("").value
(Compile / compile) := ((Compile / compile) dependsOn compileScalastyle).value

scalafmtOnCompile := true
PlayKeys.playDefaultPort := 9052

dependencyUpdatesFailBuild := false
(Compile / compile) := ((Compile / compile) dependsOn dependencyUpdates).value
dependencyUpdatesFilter -= moduleFilter(organization = "org.scala-lang")

Compile / doc / sources := Seq.empty

swaggerDomainNameSpaces := Seq("uk.gov.hmrc.channelpreferences.controllers.models.generic")
swaggerTarget := baseDirectory.value / "public"
swaggerFileName := "schema.json"
swaggerPrettyJson := true
swaggerRoutesFile := "prod.routes"
swaggerV3 := true
