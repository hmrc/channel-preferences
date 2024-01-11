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

import play.api.http.Status.OK
import play.api.libs.json.{ JsSuccess, Json }
import play.api.{ Configuration, Logger, LoggerLike }
import uk.gov.hmrc.channelpreferences.model.cds.EmailVerification
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.{ ParseError, UpstreamError }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton
class CDSEmailConnector @Inject() (config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {
  private val log: LoggerLike = Logger(this.getClass)
  val serviceUrl: String = baseUrl("customs-data-store")

  def getVerifiedEmail(taxId: String)(implicit hc: HeaderCarrier): Future[Either[PreferenceError, EmailVerification]] =
    httpClient
      .doGet(
        s"$serviceUrl/customs-data-store/eori/$taxId/verified-email",
        hc.headers(Seq("Authorization", "X-Request-Id"))
      )
      .map { resp =>
        resp.status match {
          case OK     => parseCDSVerifiedEmailResp(resp.body)
          case status => Left(UpstreamError(Option(resp.body).getOrElse(""), status))
        }
      }

  private def parseCDSVerifiedEmailResp(body: String): Either[PreferenceError, EmailVerification] =
    Try(Json.parse(body)) match {
      case Success(v) =>
        v.validate[EmailVerification] match {
          case JsSuccess(ev, _) => Right(ev)
          case _ =>
            log.warn(s"unable to parse $body")
            Left(ParseError(s"unable to parse $body"))
        }
      case Failure(e) =>
        log.error(s"cds response was invalid Json", e)
        Left(ParseError(s"cds response was invalid Json"))
    }
}
