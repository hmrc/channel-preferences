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

import cats.data.NonEmptySet
import uk.gov.hmrc.channelpreferences.controllers.model._
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences._

import scala.concurrent.Future

trait PreferenceManagementService {
  def getPreference(enrolments: NonEmptySet[Enrolment]): Future[Option[ContextualPreference]]

  def updateConsent(
    consent: Consent,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, ContextualPreference]]

  def updateNavigation(
    navigationContext: NavigationContext,
    enrolments: NonEmptySet[Enrolment]): Future[Either[PreferenceError, NavigationContext]]

  def createVerification(
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, ContextualPreference]]

  def confirm(verificationId: VerificationId): Future[Either[PreferenceError, Option[ContextualPreference]]]
}
