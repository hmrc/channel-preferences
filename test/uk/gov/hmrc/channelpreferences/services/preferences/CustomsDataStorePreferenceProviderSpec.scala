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

package uk.gov.hmrc.channelpreferences.services.preferences

import cats.syntax.either._
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import play.api.libs.json.Json
import uk.gov.hmrc.channelpreferences.connectors.CDSEmailConnector
import uk.gov.hmrc.channelpreferences.model.cds.{ Email, EmailVerification, Phone }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnsupportedChannelError
import uk.gov.hmrc.channelpreferences.model.preferences.{ CustomsServiceEnrolment, IdentifierValue }
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.EventTypes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CustomsDataStorePreferenceProviderSpec
    extends AnyFlatSpec with Matchers with ScalaFutures with IdiomaticMockito {

  behavior of "CustomsDataStorePreferenceProvider.getPreference"

  it should "return the json representation of a email verification" in new Scope {
    customsDataStorePreferenceProvider
      .getPreference(customsServiceEnrolment)
      .futureValue shouldBe Json.toJson(emailVerification).asRight

    auditConnector.sendExplicitAudit(
      EventTypes.Succeeded,
      Map(
        "transactionName" -> "Retrieve Email Address from customs-data-store",
        "email"           -> emailVerification.address.value)
    ) wasCalled once
  }

  it should "return an error for a channel type other than Email" in new Scope {
    private val unsupportedChannelEnrolment = CustomsServiceEnrolment(identifierValue, Phone)

    customsDataStorePreferenceProvider
      .getPreference(unsupportedChannelEnrolment)
      .futureValue shouldBe UnsupportedChannelError(Phone).asLeft

    auditConnector.sendExplicitAudit(*[String], *[Map[String, String]]) wasNever called
  }

  trait Scope {
    val cdsEmailConnector: CDSEmailConnector = mock[CDSEmailConnector]
    val auditConnector: AuditConnector = mock[AuditConnector]

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val identifierValue: IdentifierValue = IdentifierValue("foo")
    val customsServiceEnrolment: CustomsServiceEnrolment = CustomsServiceEnrolment(identifierValue, Email)
    val emailVerification: EmailVerification = EmailVerification(EmailAddress("foo@bar.com"), DateTime.now())

    cdsEmailConnector
      .getVerifiedEmail(customsServiceEnrolment.identifierValue.value)
      .returns(Future.successful(emailVerification.asRight))

    val customsDataStorePreferenceProvider = new CustomsDataStorePreferenceProvider(
      cdsEmailConnector,
      auditConnector
    )
  }
}
