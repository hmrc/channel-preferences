/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, urlPathMatching }
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import play.api.http.Status
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.channelpreferences.controllers.ProxyController

class ProcessBounceISpec extends ISpec {

  implicit val system: ActorSystem = ActorSystem("system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  lazy val mockServer = new WireMockServer(
    wireMockConfig()
      .dynamicHttpsPort()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true))
  )

  lazy val mockServerHttpUrl = s"http://localhost:${mockServer.port()}"

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    mockServer.stop()
  }

  override protected def afterEach(): Unit =
    mockServer.resetAll()

  trait Setup {
    val application = new GuiceApplicationBuilder()
      .configure("microservice.services.preferences.host" -> "localhost")
      .configure("microservice.services.preferences.protocol" -> "http")
      .configure("microservice.services.preferences.port" -> mockServer.port)
      .configure("metrics.enabled" -> false)
      .build()

    val proxyController = application.injector.instanceOf[ProxyController]

    mockServer.addStubMapping(
      get(urlPathMatching("/ping/ping")).willReturn(aResponse().withStatus(200).withBody("OK")).build()
    )
  }

  "A POST request to /channel-preferences/process/bounce to process incoming bounce messages from the event-hub" should {

    "return an OK(200) success status" in {
      val postData = s"""
                        |{
                        |    "subject": "bounced-email",
                        |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                        |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                        |    "timestamp" : "2021-04-07T09:46:29+00:00",
                        |    "event" : {
                        |        "event": "failed",
                        |        "emailAddress": "hmrc-customer@some-domain.org",
                        |        "detected": "2021-04-07T09:46:29+00:00",
                        |        "code": 605,
                        |        "reason": "Not delivering to previously bounced address",
                        |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                        |    }
                        |}
      """.stripMargin
      val response =
        wsClient
          .url(resource("/channel-preferences/process/bounce"))
          .withHttpHeaders(("Content-Type" -> "application/json"))
          .post(postData)
          .futureValue

      response.status mustBe Status.OK
      response.body mustBe "Bounce processed successfully for 77ed39b7-d5d8-46ed-abab-a5a8ff416dae"
    }

    "return UNSUPPORTED MEDIA TYPE (415) when request does not meet the correct media type (application/json)" in {
      val postData = s"""
                        |{
                        |    "subject": "bounced-email",
                        |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
                        |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
                        |    "timestamp" : "2021-04-07T09:46:29+00:00",
                        |    "event" : {
                        |        "event": "failed",
                        |        "emailAddress": "hmrc-customer@some-domain.org",
                        |        "detected": "2021-04-07T09:46:29+00:00",
                        |        "code": 605,
                        |        "reason": "Not delivering to previously bounced address",
                        |        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
                        |    }
                        |}
      """.stripMargin
      val responseWithInvalidContentType =
        wsClient
          .url(resource("/channel-preferences/process/bounce"))
          .withHttpHeaders(("Content-Type" -> "invalid/json"))
          .post(postData)
          .futureValue

      responseWithInvalidContentType.status mustBe (UNSUPPORTED_MEDIA_TYPE)

      val responseWithMissingContentType =
        wsClient
          .url(resource("/channel-preferences/process/bounce"))
          .post(postData)
          .futureValue

      responseWithMissingContentType.status mustBe (UNSUPPORTED_MEDIA_TYPE)
    }
  }
}
