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

package uk.gov.hmrc.channelpreferences.services.eis

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.channelpreferences.connectors.EISConnector
import uk.gov.hmrc.channelpreferences.model.eis.ItsaETMPUpdate
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

import java.util.UUID
import scala.concurrent.Future

class EISContactPreferenceServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "updateContactPreference" must {
    "return success response as returned from EIS" in new TestClass {
      when(mockConnector.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)))
        .thenReturn(Future.successful(successResponse))
      val service = new EISContactPreferenceService(mockConnector)
      service.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)).futureValue mustBe successResponse
    }

    "return bad request response as returned from EIS" in new TestClass {
      when(mockConnector.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)))
        .thenReturn(Future.successful(badRequest))
      val service = new EISContactPreferenceService(mockConnector)
      service.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)).futureValue mustBe badRequest
    }

    "return exception as returned from EIS" in new TestClass {
      when(mockConnector.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)))
        .thenReturn(Future.failed(new InternalServerException("Server down")))
      val service = new EISContactPreferenceService(mockConnector)
      assertThrows[InternalServerException](
        await(service.updateContactPreference(regime, itsaETMPUpdate, Some(correlationId)))
      )
    }

  }

  class TestClass {
    val regime: String = "itsa"
    val itsaETMPUpdate: ItsaETMPUpdate = ItsaETMPUpdate("MTDBSA", "XMIT00000064424", status = true)
    val correlationId: String = UUID.randomUUID().toString
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val successResponse: HttpResponse = HttpResponse(Status.OK, "success")
    val badRequest: HttpResponse = HttpResponse(Status.BAD_REQUEST, "bad request")
    val mockConnector: EISConnector = mock[EISConnector]
  }
}
