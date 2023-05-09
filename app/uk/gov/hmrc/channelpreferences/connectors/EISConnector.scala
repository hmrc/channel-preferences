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

package uk.gov.hmrc.channelpreferences.connectors

import play.api.Configuration
import play.api.http.HeaderNames.{ ACCEPT, AUTHORIZATION, CONTENT_TYPE, DATE, USER_AGENT }
import play.api.http.MimeTypes
import uk.gov.hmrc.http.{ HttpClient, HttpResponse }
import uk.gov.hmrc.channelpreferences.utils.CustomHeaders
import uk.gov.hmrc.channelpreferences.model.eis.{ ItsaETMPUpdate, UpdateContactPreferenceRequest }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{ ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
@Singleton
class EISConnector @Inject()(config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {

  private val serviceUrl: String = baseUrl("eis")
  private val eisBearerToken = getString("microservice.services.eis.bearer-token")
  private val eisEnvironment = getString("microservice.services.eis.environment")

  private def endpointUrl(regime: String): String = s"$serviceUrl/income-tax/customer/$regime/contact-preference"

  def updateContactPreference(
    regime: String,
    itsaEnrolment: ItsaETMPUpdate,
    correlationId: Option[String]
  ): Future[HttpResponse] = {
    val requestBody =
      UpdateContactPreferenceRequest(itsaEnrolment.identifierType, itsaEnrolment.identifier, itsaEnrolment.status)
    val headers =
      Seq(
        CONTENT_TYPE                -> MimeTypes.JSON,
        ACCEPT                      -> MimeTypes.JSON,
        AUTHORIZATION               -> s"Bearer $eisBearerToken",
        DATE                        -> DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)),
        USER_AGENT                  -> config.get[String]("appName"),
        CustomHeaders.ForwardedHost -> "Digital",
        CustomHeaders.Environment   -> eisEnvironment
      ) ++ correlationId.map(CustomHeaders.CorrelationId -> _).toList
    httpClient.doPut(endpointUrl(regime), requestBody, headers)

  }

}
