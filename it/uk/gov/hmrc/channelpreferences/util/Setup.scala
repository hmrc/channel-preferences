/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.channelpreferences.util

import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{ DefaultTestServerFactory, RunningServer }
import play.api.{ Application, Logging }
import uk.gov.hmrc.channelpreferences.model.preferences.Enrolment
import uk.gov.hmrc.integration.TestId
import uk.gov.hmrc.integration.UrlHelper.-/
import uk.gov.hmrc.integration.servicemanager.ServiceManagerClient
import uk.gov.hmrc.mongo.MongoComponent

object Setup {
  def scope(enrolments: List[Enrolment])(test: Setup => Any)(implicit testId: TestId): Unit = {
    val setup = new Setup(enrolments, testId)
    try test(setup)
    finally setup.shutdown()
  }
}

class Setup(enrolments: List[Enrolment], testId: TestId) extends ScalaFutures with EitherValues with Logging {
  val externalServices: Seq[String] = Seq("auth-login-api", "identity-verification")
  private lazy val externalServicePorts: Map[String, Int] =
    ServiceManagerClient.start(testId, externalServices)

  val externalServiceConfig: Map[String, Any] =
    externalServicePorts.foldLeft(Map.empty[String, Any])((map, servicePort) =>
      map ++ (servicePort match {
        case (serviceName, p) =>
          Map(
            s"microservice.services.$serviceName.port" -> p,
            s"microservice.services.$serviceName.host" -> "localhost"
          )
      }))

  private val application: Application = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> s"mongodb://localhost:27017/${testId.toString}")
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .configure("metrics.enabled" -> true)
    .configure("auditing.enabled" -> false)
    .configure(externalServiceConfig)
    .build()

  private val runningServer: RunningServer = DefaultTestServerFactory.start(application)
  private val port: Int = runningServer.endpoints.httpEndpoint
    .fold(throw new IllegalStateException("No HTTP port available for test server"))(_.port)

  val wsClient: WSClient = application.injector.instanceOf[WSClient]
  private val mongoComponent: MongoComponent = application.injector.instanceOf[MongoComponent]

  def externalResource(serviceName: String, path: String): String = {
    val port =
      externalServicePorts
        .getOrElse(serviceName, throw new IllegalArgumentException(s"Unknown service '$serviceName'"))
    s"http://localhost:$port/${-/(path)}"
  }

  def resource(path: String): String =
    s"http://localhost:$port/${-/(path)}"

  val authLoginUrl: String = externalResource("auth-login-api", "/government-gateway/session/login")
  val authHeader: (String, String) = buildUserToken(enrolmentsJons(enrolments))

  def buildUserToken(payload: String): (String, String) = {
    val response = wsClient
      .url(authLoginUrl)
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(payload)
      .futureValue(timeout(Span(20, Seconds)))

    ("Authorization", response.header("Authorization").get)
  }

  private def enrolmentsJons(enrolments: List[Enrolment]): String =
    s"""
       |{
       |  "credId": "1235",
       |  "affinityGroup": "Organisation",
       |  "confidenceLevel": 200,
       |  "credentialStrength": "none",
       |  "enrolments": [${enrolments.map(enrolmentJson).mkString(",")}]
       |}
     """.stripMargin

  private def enrolmentJson(enrolment: Enrolment) =
    s"""
       |{
       |  "key": "${enrolment.enrolmentQualifier.enrolmentKey.value}",
       |  "identifiers": [
       |    {
       |      "key": "${enrolment.enrolmentQualifier.identifierKey.value}",
       |      "value": "${enrolment.identifierValue.value}"
       |    }
       |  ],
       |  "state": "Activated"
       |      }
       |""".stripMargin

  private def shutdown(): Unit = {
    mongoComponent.database.drop().toFuture().futureValue
    runningServer.stopServer.close()
    application.stop().futureValue

    logger.debug(s"Stopping all external services")
    try {
      ServiceManagerClient.stop(testId, dropDatabases = true)
    } catch {
      case t: Throwable => logger.error(s"An exception occurred while stopping external services", t)
    }
  }
}
