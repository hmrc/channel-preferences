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

//import controllers.Assets.{ACCEPT, CONTENT_TYPE}
import play.api.Configuration
import play.api.http.HeaderNames.{ ACCEPT, AUTHORIZATION, CONTENT_TYPE }
import play.api.http.MimeTypes
import uk.gov.hmrc.channelpreferences.model.UpdateContactPreferenceRequest
import uk.gov.hmrc.http.HttpClient
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.{ InternalServerError, Ok }
import uk.gov.hmrc.channelpreferences.connectors.utils.CustomHeaders
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
@Singleton
class EISConnector @Inject()(config: Configuration, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends ServicesConfig(config) {

  private val serviceUrl: String = baseUrl("eis")
  private val eisBearerToken = getString("microservice.services.eis.bearer-token")
  private val eisEnvironment = getString("microservice.services.eis.environment")

  private def xxxUrl(regime: String): String = s"$serviceUrl/income-tax/customer/$regime/contact-preference"

  def updateContactPreference(regime: String, digitalChannel: Boolean, correlationId: String): Future[Result] = {
    val requestBody = UpdateContactPreferenceRequest(digitalChannel)
    val headers =
      Seq(
        CONTENT_TYPE                -> MimeTypes.JSON,
        ACCEPT                      -> MimeTypes.JSON,
        AUTHORIZATION               -> s"Bearer $eisBearerToken",
        CustomHeaders.CorrelationId -> correlationId,
        CustomHeaders.ForwardedHost -> "Digital",
        CustomHeaders.Environment   -> eisEnvironment
      )
    httpClient.doPut(xxxUrl(regime), requestBody, headers).map { response =>
      response.status match {
        case OK => Ok(response.body)
        case _  => InternalServerError(response.body)
      }
    }
  }

}
