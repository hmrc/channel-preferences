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

package uk.gov.hmrc.channelpreferences.services.entityresolver

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EntityResolverServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "confirm" must {
    "return response as returned from entity resolver" in new TestClass {
      val service = new EntityResolverService(mockConnector)
      service.confirm(entityId, itsaId).futureValue mustBe successResponse
    }
  }

  "enrolment" must {
    "return response as returned from entity resolver" in new TestClass {
      val service = new EntityResolverService(mockConnector)
      service.enrolment(requestBody).futureValue mustBe successResponse
    }
  }

  class TestClass {
    val entityId: String = UUID.randomUUID().toString
    val itsaId: String = UUID.randomUUID().toString
    val requestBody: JsValue = Json.parse("{}")
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse: HttpResponse = HttpResponse(Status.OK, "success")
    val mockConnector: EntityResolverConnector = mock[EntityResolverConnector]
    when(mockConnector.confirm(entityId, itsaId))
      .thenReturn(Future.successful(successResponse))
    when(mockConnector.enrolment(requestBody))
      .thenReturn(Future.successful(successResponse))
  }
}
