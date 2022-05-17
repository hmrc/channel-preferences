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

import cats.syntax.either._
import play.api.libs.json.JsValue
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnsupportedIdentifierKey
import uk.gov.hmrc.channelpreferences.model.preferences._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait PreferenceResolver {
  def resolveChannelPreference(channelledEnrolment: ChannelledEnrolment)(
    implicit headerCarrier: HeaderCarrier): Future[Either[PreferenceError, JsValue]]
}

object PreferenceResolver {
  def toChannelledEnrolment(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel
  ): Either[PreferenceError, ChannelledEnrolment] =
    toEnrolment(enrolmentKey, identifierKey, identifierValue).map(
      ChannelledEnrolment(_, channel)
    )

  def toEnrolment(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue
  ): Either[PreferenceError, Enrolment] =
    enrolmentKey match {
      case CustomsServiceKey =>
        identifierKey match {
          case EORINumber => CustomsServiceEnrolment(identifierValue).asRight
          case other      => UnsupportedIdentifierKey(enrolmentKey, other).asLeft
        }
      case PensionsOnlineKey =>
        identifierKey match {
          case PensionsAdministrator => PensionsAdministratorEnrolment(identifierValue).asRight
          case other                 => UnsupportedIdentifierKey(enrolmentKey, other).asLeft
        }
      case PensionsSchemePractitionerKey =>
        identifierKey match {
          case PensionsPractitioner => PensionsPractitionerEnrolment(identifierValue).asRight
          case other                => UnsupportedIdentifierKey(enrolmentKey, other).asLeft
        }
    }
}
