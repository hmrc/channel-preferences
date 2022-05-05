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

import play.api.libs.json.{ Format, JsError, JsObject, JsResult, JsValue, Json, Reads, Writes, __ }
import uk.gov.hmrc.channelpreferences.model.preferences.Updated

import java.time.{ Instant, LocalDateTime, ZoneOffset }

case class ContextPayload(
  contextId: ContextId,
  expiry: LocalDateTime,
  context: Context
)

object ContextPayload {
  implicit val contextPayloadFormat: Format[ContextPayload] = Json.format[ContextPayload]
}

object ContextPayloadMongo {

  implicit val localDateTimeReads: Reads[LocalDateTime] =
    Reads
      .at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  implicit val localDateTimeWrites: Writes[LocalDateTime] =
    Writes
      .at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  implicit object Format extends Format[Context] {
    override def writes(o: Context): JsValue = {
      implicit val updatedFormat: Format[Updated] = Json.format[Updated]
      o match {
        case c: Consent =>
          Consent.consentCustomFormat(updatedFormat).writes(c)
        case v: VerificationContext         => VerificationContext.format.writes(v)
        case cv: ConsentVerificationContext => ConsentVerificationContext.format.writes(cv)
        case c: ConfirmationContext         => ConfirmationContext.format.writes(c)
      }
    }

    override def reads(json: JsValue): JsResult[Context] = {

      implicit val localDateTimeReads: Reads[LocalDateTime] =
        Reads
          .at[String](__ \ "$date" \ "$numberLong")
          .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

      val updatedFormat: Format[Updated] = Json.format[Updated]

      json match {
        case JsObject(_) =>
          ConfirmationContext.format
            .reads(json)
            .orElse(ConsentVerificationContext.format.reads(json))
            .orElse(VerificationContext.format.reads(json))
            .orElse(Consent.consentCustomFormat(updatedFormat).reads(json))

        case other => JsError(s"expected json object for Context but got $other")
      }
    }
  }

  implicit val contextPayloadFormat: Format[ContextPayload] = Json.format[ContextPayload]

}
