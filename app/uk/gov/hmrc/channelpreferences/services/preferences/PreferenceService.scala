/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import play.api.libs.json.JsValue
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.{ Enrolment, EnrolmentKey, IdentifierKey, IdentifierValue, PreferenceError }
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferenceService @Inject() (preferenceResolver: PreferenceResolver) {
  def getChannelPreference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel
  )(implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Either[PreferenceError, JsValue]] =
    (for {
      enrolment: Enrolment <- EitherT.fromEither[Future](
                                PreferenceResolver.toEnrolment(enrolmentKey, identifierKey, identifierValue, channel)
                              )
      resolution <- EitherT(preferenceResolver.resolvePreferenceForEnrolment(enrolment))
    } yield resolution).value
}
