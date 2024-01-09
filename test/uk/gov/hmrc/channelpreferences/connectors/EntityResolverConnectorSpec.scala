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

import org.mockito.ArgumentMatchers.{ any, eq => meq }
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json, Writes }
import uk.gov.hmrc.http.{ Authorization, HeaderCarrier, HttpClient, HttpResponse, RequestId }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }

class EntityResolverConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  implicit val hc: HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization("bearer")), requestId = Some(RequestId("Id")))

  "confirm" should {
    "return the response from entity resolver for given entityId & itsaId" in new TestCase {
      when(
        mockHttpClient
          .doEmptyPost(meq("https://host:443/preferences/confirm/123/1234"), meq(Seq("Authorization" -> "bearer")))(
            any[ExecutionContext]()))
        .thenReturn(Future.successful(successResponse))
      Await.result(connector.confirm("123", "1234"), Duration.Inf) mustBe successResponse
    }
  }

  "enrolment" should {
    "post the enrolment request to entity resolver" in new TestCase {
      when(
        mockHttpClient
          .doPost(
            meq("https://host:443/preferences/enrolment"),
            meq(requestBody),
            meq(Seq("Authorization" -> "bearer")))(any[Writes[JsValue]](), any[ExecutionContext]()))
        .thenReturn(Future.successful(successResponse))
      Await.result(connector.enrolment(requestBody), Duration.Inf) mustBe successResponse
    }
  }

  trait TestCase {
    val mockHttpClient: HttpClient = mock[HttpClient]
    val ec = scala.concurrent.ExecutionContext.Implicits.global
    val connector = new EntityResolverConnector(configuration, mockHttpClient)(ec)
    val requestBody: JsValue = Json.parse("{}")
    val successResponse: HttpResponse = HttpResponse(Status.OK, "success")
  }

  val configuration: Configuration = Configuration(
    "microservice.services.entity-resolver.host"     -> "host",
    "microservice.services.entity-resolver.port"     -> 443,
    "microservice.services.entity-resolver.protocol" -> "https"
  )
}
