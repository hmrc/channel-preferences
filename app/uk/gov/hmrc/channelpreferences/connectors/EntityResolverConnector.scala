/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.{ HttpClient, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EntityResolverConnector @Inject()(config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {
  val serviceUrl: String = baseUrl("entity-resolver")

  def confirm(entityId: String, itsaId: String): Future[HttpResponse] =
    httpClient.doEmptyPost(s"$serviceUrl/preferences/confirm/$entityId/$itsaId")
}
