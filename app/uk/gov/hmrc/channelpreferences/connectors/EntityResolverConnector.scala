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
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HeaderNames, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URI
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EntityResolverConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {
  val serviceUrl: String = baseUrl("entity-resolver")

  def confirm(entityId: String, itsaId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"$serviceUrl/preferences/confirm/$entityId/$itsaId").toURL)
      .setHeader(hc.headers(Seq(HeaderNames.authorisation)): _*)
      .execute[HttpResponse]

  def enrolment(requestBody: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"$serviceUrl/preferences/enrolment").toURL)
      .withBody(requestBody)
      .setHeader(hc.headers(Seq(HeaderNames.authorisation)): _*)
      .execute[HttpResponse]
}
