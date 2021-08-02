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
import play.api.{ Configuration, Logger, LoggerLike }
import play.api.http.Status.OK
import play.api.libs.json.{ JsSuccess, Json }
import uk.gov.hmrc.channelpreferences.model.Entity
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton
class EntityResolverConnector @Inject()(config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {

  import uk.gov.hmrc.http.HttpReads.Implicits._
  private val log: LoggerLike = Logger(this.getClass)
  private val serviceUrl = baseUrl("entity-resolver")

  def resolveBy(entityId: String)(implicit hc: HeaderCarrier): Future[Entity] =
    httpClient.GET[Entity](s"$serviceUrl/entity-resolver/$entityId")

  private def parseEntityResp(body: String): Option[Entity] =
    Try(Json.parse(body)) match {
      case Success(v) =>
        v.validate[Entity] match {
          case JsSuccess(ev, _) => Some(ev)
          case _ =>
            log.warn(s"unable to parse $body")
            None
        }

      case Failure(e) =>
        log.error(s"entity resolver response was invalid Json", e)
        None
    }

  def resolveByItsa(itsaId: String)(implicit hc: HeaderCarrier): Future[Option[Entity]] =
    httpClient.doGet(s"$serviceUrl/entity-resolver/itsa/$itsaId").map { resp =>
      resp.status match {
        case OK => parseEntityResp(resp.body)
        case _  => None
      }
    }

  def update(entity: Entity)(implicit hc: HeaderCarrier): Future[Entity] =
    httpClient.PUT[Entity, Entity](
      url = s"$serviceUrl/entity-resolver/${entity._id}",
      body = entity
    )
}
