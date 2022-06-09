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

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.syntax.either._
import uk.gov.hmrc.channelpreferences.controllers.model._
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences._

import java.time.{ Instant, LocalDateTime }
import java.util.UUID
import scala.concurrent.Future

trait PreferenceManagementService {
  def getPreference(
    groupId: GroupId,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]]
  def updateConsent(
    groupId: GroupId,
    consent: ConsentContext,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]]

  def insertNavigation(
    groupId: GroupId,
    consent: NavigationContext,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]]
  def createVerification(
    groupId: GroupId,
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, ContextualPreference]]
  def confirm(
    verificationId: VerificationId,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]]
}

object PreferenceManagementService extends PreferenceManagementService {
  private val now = Instant.now
  private val id = UUID.randomUUID()

  val consent = Consent(
    DefaultConsentType,
    ConsentStatus(true),
    Updated(now),
    Version(1, 0, 0),
    List(DigitalCommunicationsPurpose))

  val consentContext: ConsentContext = ConsentContext(consent, None)

  val preferenceContext: ContextualPreference = PreferenceContext(consentContext)

  val preferenceNavigationContext: ContextualPreference = PreferenceContext(consentContext)

  private def consentVerificationContext(emailAddress: EmailAddress) = ConsentVerificationContext(
    consent,
    Verification(
      VerificationId(id),
      emailAddress,
      LocalDateTime.now()
    ),
    None
  )

  val preference: Preference = Preference(
    enrolments = NonEmptyList.of(
      Enrolment
        .fromValue("HMRC-PODS-ORG~PSAID~GB123456789")
        .valueOr(error => throw new IllegalArgumentException(error.message))),
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

  override def getPreference(
    groupId: GroupId,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)

  override def updateConsent(
    groupId: GroupId,
    consent: ConsentContext,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)

  override def createVerification(
    groupId: GroupId,
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(PreferenceContext(consentVerificationContext(emailAddress)).asRight)

  override def confirm(
    verificationId: VerificationId,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(PreferenceWithoutContext(preference).asRight)

  override def insertNavigation(
    groupId: GroupId,
    consent: NavigationContext,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)
}
