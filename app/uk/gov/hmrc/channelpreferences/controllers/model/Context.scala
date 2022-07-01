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

import play.api.libs.json.Json

import java.time.LocalDateTime
import java.util.UUID

final case class Version(
  major: Int,
  minor: Int,
  patch: Int
)

final case class Verification(id: UUID, email: String, sent: LocalDateTime)

final case class Context(consented: Consented, verification: Verification, confirm: Confirm)

final case class Consented(
  consentType: String,
  status: Boolean,
  created: LocalDateTime,
  version: Version,
  purposes: List[String]
)

final case class Confirm(id: UUID, started: LocalDateTime)

final case class ContextPayload(key: String, resourcePath: String, expiry: LocalDateTime, context: Context)

object ContextPayload {
  implicit val versionFormat = Json.format[Version]
  implicit val consentedFormat = Json.format[Consented]
  implicit val confirmFormat = Json.format[Confirm]
  implicit val verificationFormat = Json.format[Verification]
  implicit val contextReadsFormat = Json.format[Context]
  implicit val contextPayloadFormat = Json.format[ContextPayload]
}
