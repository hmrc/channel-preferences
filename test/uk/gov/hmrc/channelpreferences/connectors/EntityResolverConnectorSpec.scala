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

package uk.gov.hmrc.channelpreferences.connectors

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.{ NOT_FOUND, OK }
import uk.gov.hmrc.channelpreferences.model.Entity
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpResponse }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class EntityResolverConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val entityResolver = Entity("entityId", None, Some("blabla"), Some("yoyo"))
  private val validEntityResolver = """{"_id":"entityId","nino":"blabla", "itsa": "yoyo"}"""
  private val invalidEntityResolver = "{}"

  "resolveByItsa" should {
    "return the entity resolver when it exists" in new TestCase {
      when(mockHttpClient.doGet("http://localhost:8015/entity-resolver/itsa/itsaId")(hc, global))
        .thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(OK)
      when(mockHttpResponse.body).thenReturn(validEntityResolver)
      Await.result(connector.resolveByItsa("itsaId"), Duration.Inf) mustBe Some(entityResolver)
    }

    "return None if the entity resolver is not found" in new TestCase {
      when(mockHttpClient.doGet("http://localhost:8015/entity-resolver/itsa/itsaId")(hc, global))
        .thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(NOT_FOUND)
      connector.resolveByItsa("itsaId").futureValue mustBe None
    }

    "return None if Entity resolver returns invalid Json response" in new TestCase {
      when(mockHttpClient.doGet("http://localhost:8015/entity-resolver/itsa/itsaId")(hc, global))
        .thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(OK)
      when(mockHttpResponse.body).thenReturn(invalidEntityResolver)
      connector.resolveByItsa("itsaId").futureValue mustBe None
    }

  }

  trait TestCase {
    val mockHttpClient: HttpClient = mock[HttpClient]
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val connector = new EntityResolverConnector(configuration, mockHttpClient)(global)

  }

  val configuration: Configuration = Configuration(
    "microservice.services.entity-resolver.host"     -> "localhost",
    "microservice.services.entity-resolver.port"     -> 8015,
    "microservice.services.entity-resolver.protocol" -> "http"
  )

}
