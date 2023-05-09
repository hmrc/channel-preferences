/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.services.entityresolver

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
import play.api.test.Helpers._
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EntityResolverServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "confirm" must {
    "return success response as returned from entity resolver" in new TestClass {
      when(mockConnector.confirm(entityId, itsaId))
        .thenReturn(Future.successful(successResponse))
      val service = new EntityResolverService(mockConnector)
      service.confirm(entityId, itsaId).futureValue mustBe successResponse
    }

    "return bad request response as returned from entity resolver" in new TestClass {
      when(mockConnector.confirm(entityId, itsaId))
        .thenReturn(Future.successful(badRequest))
      val service = new EntityResolverService(mockConnector)
      service.confirm(entityId, itsaId).futureValue mustBe badRequest
    }

    "return exception as returned from entity resolver" in new TestClass {
      when(mockConnector.confirm(entityId, itsaId))
        .thenReturn(Future.failed(new InternalServerException("Server down")))
      val service = new EntityResolverService(mockConnector)
      assertThrows[InternalServerException](
        await(service.confirm(entityId, itsaId))
      )
    }
  }

  "enrolment" must {
    "return success response as returned from entity resolver" in new TestClass {
      when(mockConnector.enrolment(requestBody))
        .thenReturn(Future.successful(successResponse))
      val service = new EntityResolverService(mockConnector)
      service.enrolment(requestBody).futureValue mustBe successResponse
    }

    "return bad request response as returned from entity resolver" in new TestClass {
      when(mockConnector.enrolment(requestBody))
        .thenReturn(Future.successful(badRequest))
      val service = new EntityResolverService(mockConnector)
      service.enrolment(requestBody).futureValue mustBe badRequest
    }

    "return exception as returned from entity resolver" in new TestClass {
      when(mockConnector.enrolment(requestBody))
        .thenReturn(Future.failed(new InternalServerException("Server down")))
      val service = new EntityResolverService(mockConnector)
      assertThrows[InternalServerException](
        await(service.enrolment(requestBody))
      )
    }
  }

  class TestClass {
    val entityId: String = UUID.randomUUID().toString
    val itsaId: String = UUID.randomUUID().toString
    val requestBody: JsValue = Json.parse("{}")
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse: HttpResponse = HttpResponse(Status.OK, "success")
    val badRequest: HttpResponse = HttpResponse(Status.BAD_REQUEST, "bad request")
    val mockConnector: EntityResolverConnector = mock[EntityResolverConnector]
  }
}
