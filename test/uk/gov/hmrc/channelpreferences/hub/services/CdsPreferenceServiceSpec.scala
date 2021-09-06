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

package uk.gov.hmrc.channelpreferences.hub.services

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ times, verify }
import scala.concurrent.ExecutionContext
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ ACCEPTED, NOT_IMPLEMENTED }
import uk.gov.hmrc.channelpreferences.connectors.CDSEmailConnector
import uk.gov.hmrc.channelpreferences.hub.cds.model.{ Email, EmailVerification, Sms }
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreferenceService
import uk.gov.hmrc.channelpreferences.audit.Auditing
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CdsPreferenceServiceSpec extends PlaySpec with ScalaFutures with MockitoSugar with Auditing {
  override val auditConnector: AuditConnector = mock[AuditConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val validEnrolmentKey = "HMRC-CUS-ORG"
  private val validTaxIdName = "EORINumber"
  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), new DateTime(1987, 3, 20, 1, 2, 3))

  "getPreference" should {
    "return the EmailVerification if found" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, auditConnector)
      when(mockEmailConnector.getVerifiedEmail("123")).thenReturn(Future.successful(Right(emailVerification)))
      service.getPreference(Email, validEnrolmentKey, validTaxIdName, "123").futureValue mustBe
        Right(emailVerification)
    }

    "audit EmailVerification if successful" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, mock[AuditConnector])
      when(mockEmailConnector.getVerifiedEmail("123")).thenReturn(Future.successful(Right(emailVerification)))

      service.getPreference(Email, validEnrolmentKey, validTaxIdName, "123").futureValue

      verify(auditConnector, times(1))
        .sendExplicitAudit(any[String], any[Map[String, String]])(any[HeaderCarrier], any[ExecutionContext])
    }

    "return Not Implemented if channel is not Email" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, auditConnector)
      service.getPreference(Sms, validEnrolmentKey, validTaxIdName, "123").futureValue mustBe
        Left(NOT_IMPLEMENTED)
    }

    s"return Not Implemented if the Enrolement key is not $validEnrolmentKey" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, auditConnector)
      service.getPreference(Email, "HMRC", validTaxIdName, "123").futureValue mustBe
        Left(NOT_IMPLEMENTED)
    }

    s"return Not Implemented if the Tax ID name is not $validTaxIdName" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, auditConnector)
      service.getPreference(Email, validEnrolmentKey, "taxID", "123").futureValue mustBe
        Left(NOT_IMPLEMENTED)
    }

    "propagate any Non OK status returned from CDS" in new TestCase {
      private val service = new CdsPreferenceService(mockEmailConnector, auditConnector)
      when(mockEmailConnector.getVerifiedEmail("123")).thenReturn(Future.successful(Left(ACCEPTED)))
      service.getPreference(Email, validEnrolmentKey, validTaxIdName, "123").futureValue mustBe
        Left(ACCEPTED)
    }

  }

  trait TestCase {
    val mockEmailConnector: CDSEmailConnector = mock[CDSEmailConnector]
  }
}
