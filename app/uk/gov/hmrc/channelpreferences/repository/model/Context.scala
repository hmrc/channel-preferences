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

package uk.gov.hmrc.channelpreferences.repository.model

import play.api.libs.json.Json
import java.time.LocalDateTime
import java.util.UUID

case class Verification(
  id: UUID,
  email: String,
  sent: LocalDateTime
)

object Verification {
  implicit val verificationFormat = Json.format[Verification]
}

case class Confirm(
  id: UUID,
  started: LocalDateTime
)

object Confirm {
  implicit val confirmFormat = Json.format[Confirm]
}

case class Consented(
  consentType: String,
  status: Boolean,
  created: LocalDateTime,
  version: Version,
  purposes: List[Purpose.Value]
)

object Consented {
  implicit val consentedFormat = Json.format[Consented]
}

case class Context(
  consented: Consented,
  verification: Verification,
  confirm: Confirm
)

object Context {
  implicit val contextFormat = Json.format[Context]
}

case class ContextPayload(
  key: String,
  resourcePath: String,
  expiry: LocalDateTime,
  context: Context
)

object ContextPayload {
  implicit val contextPayloadFormat = Json.format[ContextPayload]
}
