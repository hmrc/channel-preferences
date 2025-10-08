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
import play.api.libs.json.{ JsValue, Json }
import play.api.test.Helpers.*
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.{ Authorization, HeaderCarrier, HttpResponse, RequestId }
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URL
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }

class EntityResolverConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization("bearer")), requestId = Some(RequestId("Id")))
  implicit val ec: ExecutionContext = ExecutionContext.global

  "confirm" should {
    "return the response from entity resolver for given entityId & itsaId" in new TestCase {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(successResponse))

      Await.result(connector.confirm("123", "1234"), Duration.Inf) mustBe successResponse
    }
  }

  "enrolment" should {
    "post the enrolment request to entity resolver" in new TestCase {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(successResponse))

      Await.result(connector.enrolment(requestBody), Duration.Inf) mustBe successResponse
    }
  }

  "processItsa" should {
    "return the successful response when request is processed successfully" in new TestCase {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(successResponse))

      val result: HttpResponse = await(connector.processItsa(requestBody))

      result mustBe successResponse
    }

    "return the failure response when request is not processed successfully" in new TestCase {
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(failureResponse))

      val result: HttpResponse = await(connector.processItsa(requestBody))

      result mustBe failureResponse
    }
  }

  trait TestCase {
    val configuration: Configuration = Configuration(
      "microservice.services.entity-resolver.host"     -> "host",
      "microservice.services.entity-resolver.port"     -> 443,
      "microservice.services.entity-resolver.protocol" -> "https"
    )

    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val connector = new EntityResolverConnector(configuration, mockHttpClient)(ec)
    val requestBody: JsValue = Json.parse("{}")

    val successResponse: HttpResponse = HttpResponse(Status.OK, "success")
    val failureResponse: HttpResponse = HttpResponse(Status.BAD_REQUEST)
  }
}
