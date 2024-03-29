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
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelPreferencesError, Event, PreferencesConnectorError }
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferencesConnector @Inject() (configuration: Configuration, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends ServicesConfig(configuration) {
  val serviceUrl = baseUrl("preferences")

  def update(event: Event): Future[Either[ChannelPreferencesError, String]] =
    httpClient
      .doPost(s"$serviceUrl/preferences/email/bounce", event)
      .map(response =>
        response.status match {
          case Status.OK => Right(response.body)
          case e         => Left(PreferencesConnectorError(s"Error getting success code from preferences $e"))
        }
      )
}
