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

package uk.gov.hmrc.channelpreferences.audit

import akka.stream.Materializer
import akka.stream.testkit.NoMaterializer
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.mvc.{ Headers, ResponseHeader, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.{ CONTENT_TYPE, LOCATION, USER_AGENT }
import play.mvc.Http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{ ControllerConfig, ControllerConfigs, DefaultHttpAuditEvent }

import scala.concurrent.ExecutionContext.Implicits.global

class AuditFilterSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val configuration: Configuration = Configuration(
    "microservice.services.customs-data-store.host" -> "host"
  )
  val controllerConfigs = ControllerConfigs(Map("testController" -> ControllerConfig()))
  val auditConnector = mock[AuditConnector]
  val httpAuditEvent = new DefaultHttpAuditEvent("test")
  val mat: Materializer = NoMaterializer

  class testAuditFilter
      extends DefaultFrontendAuditFilter(configuration, controllerConfigs, auditConnector, httpAuditEvent, mat)
      with AuditFilter {
    val buildRequestDetailsTest =
      buildRequestDetails(
        FakeRequest(
          "GET",
          "/test",
          Headers(USER_AGENT -> "test", CONTENT_TYPE -> "application/x-www-form-urlencoded"),
          "body"),
        "request-body")

    val buildResponseDetailsTest =
      buildResponseDetails(ResponseHeader(Status.OK, Map("User-Agent" -> "test", LOCATION -> "test")))

    def filterResponseBodyTest(contentType: Option[String] = None) =
      filterResponseBody(
        Result(ResponseHeader(Status.OK), HttpEntity.Strict(ByteString.empty, contentType)),
        ResponseHeader(Status.OK),
        "response-body")
  }

  val filter = new testAuditFilter

  "AuditFilter" must {
    "define the controllerNeedsAuditing" in {
      filter.controllerNeedsAuditing("test") mustBe true
    }

    "define the dataEvent" in {
      filter
        .dataEvent("test", "test-transaction", FakeRequest("GET", "/test"), Map.empty)
        .auditSource mustBe "test"
    }

    "build request details" in {
      filter.buildRequestDetailsTest mustBe Map(
        "host"              -> "-",
        "queryString"       -> "-",
        "deviceFingerprint" -> "-",
        "port"              -> "-",
        "requestBody"       -> "request-body")
    }

    "build response details" in {
      filter.buildResponseDetailsTest mustBe Map("Location" -> "test")
    }

    "filter response body" in {
      filter.filterResponseBodyTest() mustBe "response-body"
      filter.filterResponseBodyTest(Some("text/html")) mustBe "<HTML>...</HTML>"
    }
  }
}
