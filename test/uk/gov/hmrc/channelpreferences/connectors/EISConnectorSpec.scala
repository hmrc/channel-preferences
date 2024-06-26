/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{ JsObject, Json, Writes }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import play.api.test.Helpers.*
import uk.gov.hmrc.channelpreferences.model.eis.ItsaETMPUpdate
import uk.gov.hmrc.channelpreferences.model.preferences.Event
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }

import java.net.URL
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class EISConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar with EitherValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "EISConnector.updateContactPreference" must {
    "return forward the response of the httpCall when succeed" in new TestCase {
      val connector = new EISConnector(configuration, httpClientMock)
      val itsaETMPUpdate = ItsaETMPUpdate("MTDBSA", "XMIT00000064424", true)
      val result =
        connector.updateContactPreference("itsa", itsaETMPUpdate, Some("correlationId")).futureValue

      result.status mustBe OK
      result.json mustBe successBody
    }

    "return INTERNAL_SERVER_ERROR when the call to EIS fails" in new TestCase {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpUnhappyResponseMock))
      val connector = new EISConnector(configuration, httpClientMock)

      val itsaETMPUpdate = ItsaETMPUpdate("MTDBSA", "XMIT00000064424", true)
      val result =
        connector.updateContactPreference("itsa", itsaETMPUpdate, Some("correlationId")).futureValue

      result.status mustBe BAD_REQUEST
      result.json mustBe failureBody
    }

  }

  class TestCase {
    val httpClientMock = mock[HttpClientV2]
    val requestBuilder = mock[RequestBuilder]
    val successBody: JsObject = Json.obj("processingDate" -> "2021-09-07T14:39:51.507Z", "status" -> "OK")
    val httpResponseMock: HttpResponse =
      HttpResponse(OK, successBody, Map[String, Seq[String]]())
    val failureBody =
      Json.parse("""{
          "failures": [
              {
                "code": "INVALID_REGIME",
                "reason": "Submission has not passed validation. Invalid regime."
              },
              {
                "code": "INVALID_CORRELATIONID",
                "reason": "Submission has not passed validation. Invalid header CorrelationId."
                }
          ]
          }""")
    val httpUnhappyResponseMock: HttpResponse =
      HttpResponse(BAD_REQUEST, failureBody, Map[String, Seq[String]]())

    when(httpClientMock.put(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(httpResponseMock))

    val configuration: Configuration = Configuration(
      "appName"                                -> "channel-preferences",
      "microservice.services.eis.host"         -> "localhost",
      "microservice.services.eis.port"         -> 8088,
      "microservice.services.eis.bearer-token" -> "bearerToken",
      "microservice.services.eis.environment"  -> "dev"
    )
    val event = Event(UUID.randomUUID(), "subject", "groupId", LocalDateTime.now(), Json.parse("{}"))
  }
}
