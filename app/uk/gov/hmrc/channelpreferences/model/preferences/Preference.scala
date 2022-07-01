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

package uk.gov.hmrc.channelpreferences.model.preferences

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json }
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ConsentVerificationContext, ContextId, Verification }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.StateTransitionError
import uk.gov.hmrc.channelpreferences.utils.Clock

case class Preference(
  enrolments: NonEmptyList[Enrolment],
  created: Created,
  consents: NonEmptyList[Consent],
  emailPreferences: List[EmailPreference],
  status: Status
)

object Preference {
  implicit val format: Format[Preference] = Json.format[Preference]

  implicit class PreferenceOps(val preference: Preference) extends AnyVal {
    def confirm(verification: Verification): Either[PreferenceError, Preference] =
      preference.emailPreferences
        .find(_.email == verification.email)
        .map(_.copy(contactable = Contactable(true)))
        .toRight(StateTransitionError(s"Missing email preference for verification"))
        .map(emailPreference =>
          preference.copy(emailPreferences = emailPreference :: preference.emailPreferences.filterNot(
            _.email == verification.email)))
  }

  def defaultConfirmedPreference(
    contextId: ContextId,
    consentVerificationContext: ConsentVerificationContext,
    clock: Clock
  ): Preference =
    Preference(
      contextId.enrolments,
      Created(clock),
      NonEmptyList.of(consentVerificationContext.consented),
      List(
        EmailPreference(
          PrimaryIndex,
          consentVerificationContext.verification.email,
          TextPlain,
          EnglishLanguage,
          Contactable(true),
          List(DigitalCommunicationsPurpose)
        )),
      Active
    )
}
