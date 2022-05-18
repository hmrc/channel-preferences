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

import cats.syntax.either._
import cats.syntax.parallel._
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.ParseError

case class ChannelledEnrolment(
  enrolment: Enrolment,
  channel: Channel
)

object ChannelledEnrolment {
  def fromValue(value: String): Either[PreferenceError, ChannelledEnrolment] =
    value.split(Enrolment.Separator) match {
      case Array(enrolmentKey, identifierKey, identifierValue, channelValue) =>
        fromQuad(enrolmentKey, identifierKey, identifierValue, channelValue)
      case anotherShape =>
        ParseError(s"expected 4 values for a channelled enrolment, but got ${anotherShape.mkString(" | ")}").asLeft
    }

  def fromQuad(
    enrolmentKey: String,
    identifierKey: String,
    identifierValue: String,
    channelValue: String): Either[PreferenceError, ChannelledEnrolment] = {
    val enrolment = Enrolment.fromTriplet(enrolmentKey, identifierKey, identifierValue)
    val channel = Channel.channelFromName(channelValue).leftMap(ParseError)

    (enrolment, channel).parMapN(ChannelledEnrolment(_, _))
  }

  def value(channelledEnrolment: ChannelledEnrolment): String =
    s"${channelledEnrolment.enrolment.value}${Enrolment.Separator}${channelledEnrolment.channel.name}"
}
