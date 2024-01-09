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

package uk.gov.hmrc.channelpreferences.services.preferences

import cats.data.EitherT
import cats.syntax.either._
import play.api.Logger
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.channelpreferences.audit.Auditing
import uk.gov.hmrc.channelpreferences.connectors.CDSEmailConnector
import uk.gov.hmrc.channelpreferences.model.cds.{ Email, EmailVerification }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnsupportedChannelError
import uk.gov.hmrc.channelpreferences.model.preferences.{ CustomsServiceEnrolment, PreferenceError }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class CustomsDataStorePreferenceProvider @Inject()(
  cdsEmailConnector: CDSEmailConnector,
  override val auditConnector: AuditConnector
)(implicit executionContext: ExecutionContext)
    extends PreferenceProvider[CustomsServiceEnrolment] with Auditing {

  private val logger = Logger(this.getClass)

  override def getPreference(enrolment: CustomsServiceEnrolment)(
    implicit headerCarrier: HeaderCarrier): Future[Either[PreferenceError, JsValue]] =
    enrolment.channel match {
      case Email =>
        EitherT(cdsEmailConnector.getVerifiedEmail(enrolment.identifierValue.value))
          .map(audit)
          .map(Json.toJson(_))
          .value

      case unsupportedChannel =>
        logger.error(s"channel $unsupportedChannel not implemented")
        Future.successful(UnsupportedChannelError(unsupportedChannel).asLeft)
    }

  private def audit(emailVerification: EmailVerification)(implicit headerCarrier: HeaderCarrier): EmailVerification = {
    auditRetrieveEmail(emailVerification.address)
    emailVerification
  }
}
