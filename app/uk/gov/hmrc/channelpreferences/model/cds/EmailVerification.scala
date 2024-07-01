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

package uk.gov.hmrc.channelpreferences.model.cds

import play.api.libs.json._
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress

import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.util.{ Failure, Success, Try }

final case class EmailVerification(address: EmailAddress, timestamp: Instant)

object EmailVerification {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

  private object EmailAddressReads extends Reads[EmailAddress] {
    def reads(json: JsValue): JsResult[EmailAddress] = json match {
      case JsString(s) =>
        Try(EmailAddress(s)) match {
          case Success(v) => JsSuccess(v)
          case Failure(e) => JsError(e.getMessage)
        }
      case _ => JsError("Uable to parse email address")
    }
  }

  private object EmailAddressWrites extends Writes[EmailAddress] {
    def writes(e: EmailAddress): JsString = JsString(e.value)
  }

  val dateTimeWithMillis: DateTimeFormatter =
    DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneOffset.UTC)

  implicit val instantWrites: Format[Instant] =
    Format(Reads.DefaultInstantReads, Writes.temporalWrites[Instant, DateTimeFormatter](dateTimeWithMillis))

  implicit val emailReads: Reads[EmailAddress] = EmailAddressReads
  implicit val emailAddressFormat: Format[EmailAddress] = Format(EmailAddressReads, EmailAddressWrites)

  implicit val emailVerificationFormat: OFormat[EmailVerification] = Json.format[EmailVerification]

}
