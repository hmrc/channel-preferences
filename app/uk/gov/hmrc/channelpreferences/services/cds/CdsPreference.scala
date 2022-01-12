/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.services.cds

import cats.data.EitherT
import com.google.inject.ImplementedBy
import play.api.http.Status.NOT_IMPLEMENTED
import play.api.{ Logger, LoggerLike }
import uk.gov.hmrc.channelpreferences.audit.Auditing
import uk.gov.hmrc.channelpreferences.connectors.CDSEmailConnector
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, EmailVerification }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[CdsPreferenceService])
trait CdsPreference {
  def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[Int, EmailVerification]]
}

class CdsPreferenceService @Inject()(cdsEmailCon: CDSEmailConnector, override val auditConnector: AuditConnector)
    extends CdsPreference with Auditing {
  private val log: LoggerLike = Logger(this.getClass)
  private val HMRC_CUS_ORG = "HMRC-CUS-ORG"
  private val EORINumber = "EORINumber"
  def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
    c match {
      case Email => emailPreference(enrolmentKey, taxIdName, taxIdValue)
      case _ =>
        log.error(s"channel $c not implemented")
        Future.successful(Left(NOT_IMPLEMENTED))
    }

  private def emailPreference(enrolmentKey: String, taxIdName: String, taxIdValue: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[Int, EmailVerification]] =
    (enrolmentKey, taxIdName) match {
      case (HMRC_CUS_ORG, EORINumber) =>
        val emailVerification = EitherT(cdsEmailCon.getVerifiedEmail(taxIdValue))
        emailVerification.map(email => auditRetrieveEmail(email.address))
        emailVerification.value
      case _ =>
        log.error(s"($enrolmentKey,$taxIdName) not supported")
        Future.successful(Left(NOT_IMPLEMENTED))
    }
}
