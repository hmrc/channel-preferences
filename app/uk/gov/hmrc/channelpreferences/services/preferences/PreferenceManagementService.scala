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

import cats.data.NonEmptyList
import cats.syntax.either._
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ConsentVerificationContext, ContextualPreference, PreferenceContext, PreferenceWithoutContext, Verification, VerificationId, Version }
import uk.gov.hmrc.channelpreferences.model.preferences._

import java.time.{ Instant, LocalDateTime }
import java.util.UUID
import scala.concurrent.Future

trait PreferenceManagementService {
  def getPreference(enrolment: Enrolment): Future[Either[PreferenceError, ContextualPreference]]
  def updateConsent(enrolment: Enrolment, consent: Consent): Future[Either[PreferenceError, ContextualPreference]]
  def createVerification(
    channelledEnrolment: ChannelledEnrolment,
    index: Index,
    emailAddress: EmailAddress
  ): Future[Either[PreferenceError, ContextualPreference]]
  def confirm(verificationId: VerificationId): Future[Either[PreferenceError, ContextualPreference]]
}

object PreferenceManagementService extends PreferenceManagementService {
  private val now = Instant.now
  private val id = UUID.randomUUID()

  val consent: Consent = Consent(
    DefaultConsentType,
    ConsentStatus(true),
    Updated(now),
    Version(1, 0, 0),
    List(DigitalCommunicationsPurpose)
  )

  val preferenceContext: ContextualPreference = PreferenceContext(consent)

  private def consentVerificationContext(emailAddress: EmailAddress) = ConsentVerificationContext(
    consent,
    Verification(
      VerificationId(id),
      emailAddress,
      LocalDateTime.now()
    )
  )

  val preference: Preference = Preference(
    enrolments = NonEmptyList.of(Enrolment.fromValue("HMRC-PODS-ORG~PSAID~GB123456789").right.get),
    created = Created(Instant.now()),
    consents = NonEmptyList.of(consent),
    emailPreferences = List(
      EmailPreference(
        index = PrimaryIndex,
        email = EmailAddress("test@test.com"),
        contentType = TextPlain,
        language = EnglishLanguage,
        contactable = Contactable(true),
        purposes = List(DigitalCommunicationsPurpose)
      )
    ),
    status = Active
  )

  override def getPreference(enrolment: Enrolment): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)

  override def updateConsent(
    enrolment: Enrolment,
    consent: Consent): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)

  override def createVerification(
    channelledEnrolment: ChannelledEnrolment,
    index: Index,
    emailAddress: EmailAddress
  ): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(PreferenceContext(consentVerificationContext(emailAddress)).asRight)

  override def confirm(verificationId: VerificationId): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(PreferenceWithoutContext(preference).asRight)
}
