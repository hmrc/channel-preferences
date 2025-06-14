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

package uk.gov.hmrc.channelpreferences.services.entityresolver

import com.google.inject.ImplementedBy
import play.api.libs.json.JsValue
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[EntityResolverService])
trait EntityResolver {
  def confirm(entityId: String, itsaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def enrolment(requestBody: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def processItsa(requestBody: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

class EntityResolverService @Inject() (entityResolverConnector: EntityResolverConnector) extends EntityResolver {

  def confirm(entityId: String, itsaId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] =
    entityResolverConnector.confirm(entityId, itsaId)

  def enrolment(requestBody: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    entityResolverConnector.enrolment(requestBody)

  def processItsa(requestBody: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    entityResolverConnector.processItsa(requestBody)
}
