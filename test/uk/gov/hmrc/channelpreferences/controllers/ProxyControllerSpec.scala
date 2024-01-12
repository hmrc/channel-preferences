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

package uk.gov.hmrc.channelpreferences.controllers

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.mvc.Results.Ok
import play.api.mvc.{ Headers, Request }
import play.api.test.Helpers
import play.api.test.Helpers.{ defaultAwaitTimeout, status }
import uk.gov.hmrc.channelpreferences.services.entityresolver.OutboundProxy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProxyControllerSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  "proxy" must {
    "return success response as returned from proxied service" in new TestClass {
      when(mockService.proxy(any[Request[Source[ByteString, _]]]))
        .thenReturn(Future.successful(Ok("success")))
      when(mockRequest.headers).thenReturn(Headers("User-Agent" -> "test"))
      val controller = new ProxyController(Helpers.stubControllerComponents(), mockService)
      val response = controller.proxy("/test").apply(mockRequest)
      status(response) mustBe OK
    }

    "return internal server error when failed to proxy the request" in new TestClass {
      when(mockService.proxy(any[Request[Source[ByteString, _]]]))
        .thenReturn(Future.failed(new Exception("proxy request failed")))
      when(mockRequest.headers).thenReturn(Headers("User-Agent" -> "test"))
      val controller = new ProxyController(Helpers.stubControllerComponents(), mockService)
      val response = controller.proxy("/test").apply(mockRequest)
      status(response) mustBe INTERNAL_SERVER_ERROR
    }

    class TestClass {
      val mockRequest: Request[Source[ByteString, _]] = mock[Request[Source[ByteString, _]]]
      val mockService: OutboundProxy = mock[OutboundProxy]
    }
  }
}
