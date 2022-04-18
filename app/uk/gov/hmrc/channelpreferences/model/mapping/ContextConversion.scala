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

package uk.gov.hmrc.channelpreferences.model.mapping

import uk.gov.hmrc.channelpreferences.controllers
import uk.gov.hmrc.channelpreferences.repository

object ContextConversion {

  def toDbContextPayload(contextPayload: controllers.model.ContextPayload): repository.model.ContextPayload = {

    val purposes: List[repository.model.Purpose.Value] =
      contextPayload.context.consented.purposes.map(p => repository.model.Purpose.withName(p))

    val version = repository.model.Version(
      major = contextPayload.context.consented.version.major,
      minor = contextPayload.context.consented.version.minor,
      patch = contextPayload.context.consented.version.patch
    )

    val consented = repository.model.Consented(
      consentType = contextPayload.context.consented.consentType,
      status = contextPayload.context.consented.status,
      created = contextPayload.context.consented.created,
      version = version,
      purposes = purposes
    )

    val verification = repository.model.Verification(
      id = contextPayload.context.verification.id,
      email = contextPayload.context.verification.email,
      sent = contextPayload.context.verification.sent
    )

    val confirm = repository.model.Confirm(
      id = contextPayload.context.confirm.id,
      started = contextPayload.context.confirm.started
    )

    val context = repository.model.Context(
      consented = consented,
      verification = verification,
      confirm = confirm
    )

    repository.model.ContextPayload(
      key = contextPayload.key,
      resourcePath = contextPayload.resourcePath,
      expiry = contextPayload.expiry,
      context = context
    )
  }

  def toApiContextPayload(contextPayload: repository.model.ContextPayload): controllers.model.ContextPayload = {

    val purposes: List[String] = contextPayload.context.consented.purposes.map(p => p.toString)

    val version = controllers.model.Version(
      major = contextPayload.context.consented.version.major,
      minor = contextPayload.context.consented.version.minor,
      patch = contextPayload.context.consented.version.patch
    )

    val consented = controllers.model.Consented(
      consentType = contextPayload.context.consented.consentType,
      status = contextPayload.context.consented.status,
      created = contextPayload.context.consented.created,
      version = version,
      purposes = purposes
    )

    val verification = controllers.model.Verification(
      id = contextPayload.context.verification.id,
      email = contextPayload.context.verification.email,
      sent = contextPayload.context.verification.sent
    )

    val confirm = controllers.model.Confirm(
      id = contextPayload.context.confirm.id,
      started = contextPayload.context.confirm.started
    )

    val context = controllers.model.Context(
      consented = consented,
      verification = verification,
      confirm = confirm
    )

    controllers.model.ContextPayload(
      key = contextPayload.key,
      resourcePath = contextPayload.resourcePath,
      expiry = contextPayload.expiry,
      context = context
    )
  }
}
