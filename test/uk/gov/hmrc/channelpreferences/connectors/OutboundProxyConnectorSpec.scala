/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ ContentTypes, HttpRequest, HttpResponse }
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ HttpExt, HttpsConnectionContext }
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.HeaderNames.{ CONTENT_LENGTH, CONTENT_TYPE }
import play.api.http.Status
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.api.mvc.{ Headers, Request, ResponseHeader }
import play.api.test.Helpers.AUTHORIZATION
import uk.gov.hmrc.http.{ Authorization, HeaderCarrier, RequestId }

import scala.collection.immutable
import scala.concurrent.Future

class OutboundProxyConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  implicit val hc: HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization("bearer")), requestId = Some(RequestId("Id")))

  "proxy" should {
    "send the request to entity resolver with the required headers" in new TestCase {
      when(mockHttp
        .singleRequest(any[HttpRequest], any[HttpsConnectionContext], any[ConnectionPoolSettings], any[LoggingAdapter]))
        .thenReturn(Future.successful(successResponse))
      connector.proxy(request(Headers())).futureValue.header mustBe ResponseHeader(
        Status.OK,
        Map("Content-Length" -> "10"))
    }
    "send the request to entity resolver with the headers passed" in new TestCase {
      when(mockHttp
        .singleRequest(any[HttpRequest], any[HttpsConnectionContext], any[ConnectionPoolSettings], any[LoggingAdapter]))
        .thenReturn(Future.successful(successResponse))
      connector
        .proxy(request(
          Headers(CONTENT_TYPE -> ContentTypes.`text/plain(UTF-8)`.value, CONTENT_LENGTH -> "10", AUTHORIZATION -> "")))
        .futureValue
        .header mustBe ResponseHeader(Status.OK, Map("Content-Length" -> "10"))
    }
  }

  trait TestCase {
    val mockHttp: HttpExt = mock[HttpExt]
    val ec = scala.concurrent.ExecutionContext.Implicits.global
    val actorsystem = ActorSystem("system")
    val connector = new OutboundProxyConnector(configuration)(actorsystem, ec) {
      override val http: HttpExt = mockHttp
    }
    def request(inboundHeaders: Headers): Request[Source[ByteString, _]] = new Request[Source[ByteString, _]] {
      override def body: Source[ByteString, _] = Source.empty

      override def connection: RemoteConnection = ???

      override def method: String = "GET"

      override def target: RequestTarget = RequestTarget("/test", "", Map.empty)

      override def version: String = ???

      override def headers: Headers = inboundHeaders

      override def attrs: TypedMap = ???
    }

    val successResponse: HttpResponse =
      HttpResponse(
        Status.OK,
        immutable.Seq(RawHeader(CONTENT_TYPE, "text/plain(UTF-8)"), RawHeader(CONTENT_LENGTH, "10")))
  }

  val configuration: Configuration = Configuration(
    "microservice.services.entity-resolver.host"     -> "host",
    "microservice.services.entity-resolver.port"     -> 443,
    "microservice.services.entity-resolver.protocol" -> "https"
  )
}