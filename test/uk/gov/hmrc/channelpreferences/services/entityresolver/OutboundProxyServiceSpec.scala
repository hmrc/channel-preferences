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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.mvc.Results.{ BadRequest, Ok }
import play.api.test.Helpers._
import uk.gov.hmrc.channelpreferences.connectors.OutboundProxyConnector
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class OutboundProxyServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "proxy" must {
    "return success response as returned from proxied service" in new TestClass {
      when(mockConnector.proxy(any[Request[Source[ByteString, _]]]))
        .thenReturn(Future.successful(Ok("success")))
      val service = new OutboundProxyService(mockConnector)
      service.proxy(request).futureValue mustBe Ok("success")
    }

    "return bad request response as returned from proxied service" in new TestClass {
      when(mockConnector.proxy(any[Request[Source[ByteString, _]]]))
        .thenReturn(Future.successful(BadRequest("bad request")))
      val service = new OutboundProxyService(mockConnector)
      service.proxy(request).futureValue mustBe BadRequest("bad request")
    }

    "return exception as returned from proxied service" in new TestClass {
      when(mockConnector.proxy(any[Request[Source[ByteString, _]]]))
        .thenReturn(Future.failed(new InternalServerException("Server down")))
      val service = new OutboundProxyService(mockConnector)
      assertThrows[InternalServerException](
        await(service.proxy(request))
      )
    }
  }

  class TestClass {
    val request: Request[Source[ByteString, _]] = mock[Request[Source[ByteString, _]]]
    val mockConnector: OutboundProxyConnector = mock[OutboundProxyConnector]
  }
}
