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

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.http.{ Authorization, HeaderCarrier, HttpResponse, RequestId }
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.Duration
import play.api.http.Status.{ NOT_FOUND, OK }
import play.api.libs.json.JsValue
import uk.gov.hmrc.channelpreferences.model.cds.EmailVerification
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.{ ParseError, UpstreamError }
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.HttpReads.Implicits.*
import java.net.{ URI, URL }
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class CDSEmailConnectorSpec extends PlaySpec with ScalaFutures {
  implicit val hc: HeaderCarrier =
    HeaderCarrier(authorization = Some(Authorization("bearer")), requestId = Some(RequestId("Id")))
  private val emailVerification =
    EmailVerification(EmailAddress("some@email.com"), Instant.parse("1987-03-20T01:02:03Z"))
  private val validEmailVerification = """{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""
  private val inValidEmailVerification = """{"add":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""

  "verifiedEmail" should {
    "return the email verification if found by CDS" in new TestCase {

      private val connector = new CDSEmailConnector(configuration, mockHttpClient)
      when(mockHttpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[JsValue])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(mockHttpResponse))

      when(mockHttpResponse.status).thenReturn(OK)
      when(mockHttpResponse.body).thenReturn(validEmailVerification)
      Await.result(connector.verifiedEmail("GB123456789000"), Duration.Inf) mustBe Right(emailVerification)
    }

    "return the status from CDS if the email verification not found" in new TestCase {
      private val connector = new CDSEmailConnector(configuration, mockHttpClient)
      when(
        mockHttpClient.post(
          ArgumentMatchers.eq(new URI("https://host:443/customs-data-store/eori/verified-email-third-party").toURL)
        )(any[HeaderCarrier])
      ).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[JsValue])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(mockHttpResponse))
      when(mockHttpResponse.status).thenReturn(NOT_FOUND)
      connector.verifiedEmail("GB123456789000").futureValue mustBe
        Left(UpstreamError("", StatusCodes.NotFound))
    }

    "return Bad Gateway if CDS returns invalid Json response" in new TestCase {
      private val connector = new CDSEmailConnector(configuration, mockHttpClient)

      when(
        mockHttpClient.post(
          ArgumentMatchers.eq(new URI("https://host:443/customs-data-store/eori/verified-email-third-party").toURL)
        )(any[HeaderCarrier])
      ).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[JsValue])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(mockHttpResponse))

      when(mockHttpResponse.status).thenReturn(OK)
      when(mockHttpResponse.body).thenReturn(inValidEmailVerification)
      connector.verifiedEmail("GB123456789000").futureValue mustBe
        Left(ParseError("""unable to parse {"add":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}"""))
    }

    "return Bad Gateway if CDS returns non Json response" in new TestCase {
      private val connector = new CDSEmailConnector(configuration, mockHttpClient)

      when(
        mockHttpClient.post(
          ArgumentMatchers.eq(new URI("https://host:443/customs-data-store/eori/verified-email-third-party").toURL)
        )(any[HeaderCarrier])
      ).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[JsValue])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any[(String, String)])).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(mockHttpResponse))

      when(mockHttpResponse.status).thenReturn(OK)
      when(mockHttpResponse.body).thenReturn("NonJsonResponse")
      connector.verifiedEmail("GB123456789000").futureValue mustBe
        Left(ParseError("cds response was invalid Json"))
    }
  }

  trait TestCase {
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder = mock[RequestBuilder]
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
  }

  val configuration: Configuration = Configuration(
    "microservice.services.customs-data-store.host"     -> "host",
    "microservice.services.customs-data-store.port"     -> 443,
    "microservice.services.customs-data-store.protocol" -> "https"
  )

}
