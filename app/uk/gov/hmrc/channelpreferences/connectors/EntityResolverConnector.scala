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

import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import uk.gov.hmrc.channelpreferences.model.Entity
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EntityResolverConnector @Inject()(config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {

  import uk.gov.hmrc.http.HttpReads.Implicits._
  private val serviceUrl = baseUrl("entity-resolver")

  def resolveBy(id: String)(implicit hc: HeaderCarrier): Future[Entity] = {
    httpClient.GET[Entity](s"$serviceUrl/entity-resolver/$id")
  }

  def update(entity: Entity)(implicit hc: HeaderCarrier): Future[Entity] = {
    httpClient.PUT[Entity, Entity](
      url = s"$serviceUrl/entity-resolver/${entity.id}",
      body = entity
    )
  }
}
