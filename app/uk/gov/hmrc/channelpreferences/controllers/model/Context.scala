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

package uk.gov.hmrc.channelpreferences.controllers.model

import play.api.libs.json._
import uk.gov.hmrc.channelpreferences.controllers.model.Consent.consentCustomFormat
import uk.gov.hmrc.channelpreferences.model.preferences.{ ConsentStatus, ConsentType, Purpose, Updated }

sealed trait Context

case class Consent(
  consentType: ConsentType,
  status: ConsentStatus,
  updated: Updated,
  version: Version,
  purposes: List[Purpose]
) extends Context

object Consent {
  implicit val updatedFormat: OFormat[Updated] = Json.format[Updated]
  def consentCustomFormat(implicit updatedFormat: Format[Updated]): OFormat[Consent] = Json.format[Consent]
}

case class VerificationContext(
  verification: Verification
) extends Context

object VerificationContext {
  implicit val format: OFormat[VerificationContext] = Json.format[VerificationContext]
}

case class ConsentVerificationContext(
  consented: Consent,
  verification: Verification
) extends Context

object ConsentVerificationContext {
  val updatedFormat: OFormat[Updated] = Json.format[Updated]
  implicit val consentFormat: OFormat[Consent] = consentCustomFormat(updatedFormat)
  implicit val format: OFormat[ConsentVerificationContext] = Json.format[ConsentVerificationContext]
}

case class ConfirmationContext(
  consented: Consent,
  verification: Verification,
  confirm: Confirm
) extends Context

object ConfirmationContext {
  implicit val updatedFormat: OFormat[Updated] = Json.format[Updated]
  implicit val consentFormat: OFormat[Consent] = consentCustomFormat
  implicit val format: OFormat[ConfirmationContext] = Json.format[ConfirmationContext]
}

object Context {
  implicit object Format extends Format[Context] {
    override def writes(o: Context): JsValue = o match {
      case c: Consent => {
        val updatedFormat: Format[Updated] = Json.valueFormat[Updated]
        val consent = Consent.consentCustomFormat(updatedFormat)
        consent.writes(c)

      }
      case v: VerificationContext         => VerificationContext.format.writes(v)
      case cv: ConsentVerificationContext => ConsentVerificationContext.format.writes(cv)
      case c: ConfirmationContext => {

        ConfirmationContext.format.writes(c)
      }
    }

    override def reads(json: JsValue): JsResult[Context] = json match {
      case JsObject(_) =>
        implicit val format: Format[Updated] = Json.valueFormat[Updated]
        ConfirmationContext.format
          .reads(json)
          .orElse(ConsentVerificationContext.format.reads(json))
          .orElse(VerificationContext.format.reads(json))
          .orElse(Consent.consentCustomFormat.reads(json))
      case other => JsError(s"expected json object for Context but got $other")
    }
  }
}
