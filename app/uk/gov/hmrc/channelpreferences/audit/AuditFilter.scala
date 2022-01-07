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
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.mvc.{ RequestHeader, ResponseHeader, Result }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.filters.CommonAuditFilter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.config.{ ControllerConfigs, HttpAuditEvent }

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuditFilter extends CommonAuditFilter with BackendHeaderCarrierProvider {
  def maskedFormFields: Seq[String]

  def applicationPort: Option[Int]

  private val textHtml = ".*(text/html).*".r

  override protected def filterResponseBody(result: Result, response: ResponseHeader, responseBody: String) =
    result.body.contentType
      .collect { case textHtml(_) => "<HTML>...</HTML>" }
      .getOrElse(responseBody)

  override protected def buildRequestDetails(requestHeader: RequestHeader, requestBody: String): Map[String, String] =
    Map(
      EventKeys.RequestBody -> stripPasswords(requestHeader.contentType, requestBody, maskedFormFields),
      "deviceFingerprint"   -> DeviceFingerprint.deviceFingerprintFrom(requestHeader),
      "host"                -> getHost(requestHeader),
      "port"                -> getPort,
      "queryString"         -> getQueryString(requestHeader.queryString)
    )

  override protected def buildResponseDetails(response: ResponseHeader): Map[String, String] =
    response.headers
      .get(HeaderNames.LOCATION)
      .map(HeaderNames.LOCATION -> _)
      .toMap

  private[audit] def getQueryString(queryString: Map[String, Seq[String]]): String =
    cleanQueryStringForDatastream(
      queryString.map { case (k, vs) => k + ":" + vs.mkString(",") }.mkString("&")
    )

  private[audit] def getHost(request: RequestHeader): String =
    request.headers.get("Host").map(_.takeWhile(_ != ':')).getOrElse("-")

  private[audit] def getPort: String =
    applicationPort.map(_.toString).getOrElse("-")

  private[audit] def stripPasswords(
    contentType: Option[String],
    requestBody: String,
    maskedFormFields: Seq[String]
  ): String =
    contentType match {
      case Some("application/x-www-form-urlencoded") =>
        maskedFormFields.foldLeft(requestBody)((maskedBody, field) =>
          maskedBody.replaceAll(field + """=.*?(?=&|$|\s)""", field + "=#########"))
      case _ => requestBody
    }

  private def cleanQueryStringForDatastream(queryString: String): String =
    queryString.trim match {
      case ""    => "-"
      case ":"   => "-" // play 2.5 FakeRequest now parses an empty query string into a two empty string params
      case other => other
    }
}

class DefaultFrontendAuditFilter @Inject()(
  override val config: Configuration,
  controllerConfigs: ControllerConfigs,
  override val auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  override val mat: Materializer
)(implicit protected val ec: ExecutionContext)
    extends AuditFilter {

  override val maskedFormFields: Seq[String] = Seq.empty

  override val applicationPort: Option[Int] = None

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfigs.controllerNeedsAuditing(controllerName)

  override def dataEvent(
    eventType: String,
    transactionName: String,
    request: RequestHeader,
    detail: Map[String, String]
  )(implicit hc: HeaderCarrier): DataEvent =
    httpAuditEvent.dataEvent(eventType, transactionName, request, detail)
}
