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
import uk.gov.hmrc.channelpreferences.repository
import uk.gov.hmrc.channelpreferences.repository.model.Purpose

import java.time.LocalDateTime
import java.util.UUID

final case class Version(
  major: Int,
  minor: Int,
  patch: Int
)

final case class Verification(
  id: UUID,
  email: String,
  sent: LocalDateTime
)

final case class Context(
  consented: Consented,
  verification: Verification,
  confirm: Confirm
)

final case class Consented(
  consentType: String,
  status: Boolean,
  created: LocalDateTime,
  version: Version,
  purposes: List[String]
)

final case class Confirm(
  id: UUID,
  started: LocalDateTime
)

final case class ContextPayload(
  key: String,
  resourcePath: String,
  expiry: LocalDateTime,
  context: Context
) {

  def toDbContextPayload(): repository.model.ContextPayload = {

    val purposes: List[repository.model.Purpose.Value] = this.context.consented.purposes.map(p => Purpose.withName(p))

    val version = repository.model.Version(
      major = this.context.consented.version.major,
      minor = this.context.consented.version.minor,
      patch = this.context.consented.version.patch
    )

    val consented = repository.model.Consented(
      consentType = this.context.consented.consentType,
      status = this.context.consented.status,
      created = this.context.consented.created,
      version = version,
      purposes = purposes
    )

    val verification = repository.model.Verification(
      id = this.context.verification.id,
      email = this.context.verification.email,
      sent = this.context.verification.sent
    )

    val confirm = repository.model.Confirm(
      id = this.context.confirm.id,
      started = this.context.confirm.started
    )

    val context = repository.model.Context(
      consented = consented,
      verification = verification,
      confirm = confirm
    )
    repository.model.ContextPayload(
      key = this.key,
      resourcePath = this.resourcePath,
      expiry = this.expiry,
      context = context
    )
  }
}

object ContextPayload {
  implicit val versionFormat = Json.format[Version]
  implicit val consentedFormat = Json.format[Consented]
  implicit val confirmFormat = Json.format[Confirm]
  implicit val verificationFormat = Json.format[Verification]
  implicit val contextReadsFormat = Json.format[Context]
  implicit val contextPayloadFormat = Json.format[ContextPayload]
}
