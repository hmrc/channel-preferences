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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.channelpreferences.model.preferences.{ Event, PreferencesConnectorError }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URL
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferencesConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "processBounce" must {
    "return response with eventId" in new TestCase {
      val connector = new PreferencesConnector(configuration, httpClientMock)
      connector.update(event).futureValue mustBe Right("bounce processed for 4ebbc776-a4ce-11ee-a3ad-2822aa445514")
    }

    "return PreferencesConnectorError if response is BAD_REQUEST" in new TestCase {
      when(httpClientMock.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpUnhappyResponseMock))

      val connector = new PreferencesConnector(configuration, httpClientMock)

      connector.update(event).futureValue mustBe Left(
        PreferencesConnectorError("Error getting success code from preferences 400")
      )
    }
  }

  class TestCase {
    val httpClientMock = mock[HttpClientV2]
    val requestBuilder = mock[RequestBuilder]
    val httpResponseMock: HttpResponse =
      HttpResponse(Status.OK, "bounce processed for 4ebbc776-a4ce-11ee-a3ad-2822aa445514")
    val httpUnhappyResponseMock: HttpResponse = HttpResponse(Status.BAD_REQUEST, "")

    when(httpClientMock.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponseMock))

    val configuration: Configuration = Configuration(
      "microservice.services.preferences.host"     -> "localhost",
      "microservice.services.preferences.port"     -> 8025,
      "microservice.services.preferences.protocol" -> "http"
    )
    val event = Event(UUID.randomUUID(), "subject", "groupId", LocalDateTime.now(), Json.parse("{}"))
  }
}
