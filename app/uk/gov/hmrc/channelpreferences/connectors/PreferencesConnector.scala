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

import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelPreferencesError, Event, PreferencesConnectorError }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URI
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferencesConnector @Inject() (configuration: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends ServicesConfig(configuration) {
  val serviceUrl: String = baseUrl("preferences")

  def update(event: Event)(implicit hc: HeaderCarrier): Future[Either[ChannelPreferencesError, String]] =
    httpClient
      .post(new URI(s"$serviceUrl/preferences/email/bounce").toURL)
      .withBody(Json.toJson(event))
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case Status.OK => Right(response.body)
          case e         => Left(PreferencesConnectorError(s"Error getting success code from preferences $e"))
        }
      )
}
