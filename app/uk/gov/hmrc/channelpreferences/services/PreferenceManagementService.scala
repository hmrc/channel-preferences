/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.services

import cats.syntax.either._
import uk.gov.hmrc.channelpreferences.controllers.model.Context.Consent
import uk.gov.hmrc.channelpreferences.controllers.model.ContextualPreference.PreferenceContext
import uk.gov.hmrc.channelpreferences.controllers.model.{ ContextualPreference, Verification, Version }
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelledEnrolment, ConsentStatus, DefaultConsentType }
import uk.gov.hmrc.channelpreferences.model.preferences.{ DigitalCommunicationsPurpose, Enrolment, Index, PreferenceError, Updated }

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

trait PreferenceManagementService {
  def getPreference(enrolment: Enrolment): Future[Either[PreferenceError, ContextualPreference]]
  def updateConsent(enrolment: Enrolment, consent: Consent): Future[Either[PreferenceError, ContextualPreference]]
  def createVerification(
    channelledEnrolment: ChannelledEnrolment,
    index: Index,
    verification: Verification): Future[Either[PreferenceError, ContextualPreference]]
  def confirm(verificationId: UUID): Future[Either[PreferenceError, ContextualPreference]]
}

object PreferenceManagementService extends PreferenceManagementService {
  private val now = Instant.now

  val preferenceContext: ContextualPreference = PreferenceContext(
    Consent(
      DefaultConsentType,
      ConsentStatus(true),
      Updated(now),
      Version(1, 0, 0),
      List(DigitalCommunicationsPurpose)
    )
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
    verification: Verification): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)

  override def confirm(verificationId: UUID): Future[Either[PreferenceError, ContextualPreference]] =
    Future.successful(preferenceContext.asRight)
}
