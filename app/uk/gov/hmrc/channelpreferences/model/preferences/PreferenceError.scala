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

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import cats.Semigroup
import cats.data.NonEmptyList
import uk.gov.hmrc.channelpreferences.model.cds.Channel

sealed abstract class PreferenceError(val message: String, val statusCode: StatusCode)

object PreferenceError {
  implicit class NonEmptyListOps[T](private val list: NonEmptyList[T]) {
    def asString: String = list.toList.mkString(" | ")
  }

  implicit object PreferenceErrorSemigroup extends Semigroup[PreferenceError] {
    override def combine(left: PreferenceError, right: PreferenceError): PreferenceError =
      PreferenceErrors(toNonEmptyList(left) concatNel toNonEmptyList(right))

    private def toNonEmptyList(error: PreferenceError): NonEmptyList[PreferenceError] = error match {
      case PreferenceErrors(values) => values
      case _                        => NonEmptyList.of(error)
    }
  }

  case class PreferenceErrors(values: NonEmptyList[PreferenceError])
      extends PreferenceError(values.map(_.message).asString, values.map(_.statusCode).toList.maxBy(_.intValue))

  case class UpstreamParseError(override val message: String)
      extends PreferenceError(
        message,
        StatusCodes.BadGateway
      )

  case class ParseError(override val message: String)
      extends PreferenceError(
        message,
        StatusCodes.BadRequest
      )

  case class UpstreamError(
    override val message: String,
    override val statusCode: StatusCode
  ) extends PreferenceError(message, statusCode)

  case class UnsupportedChannelError(channel: Channel)
      extends PreferenceError(
        s"Channel: ${channel.name}, not implemented.",
        StatusCodes.NotImplemented
      )
}
