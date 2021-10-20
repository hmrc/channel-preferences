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

package uk.gov.hmrc.channelpreferences.model

import play.api.libs.json.Json

final case class StatusUpdate(enrolment: String, status: Boolean) {
  def getItsaETMPUpdate: Either[String, ItsaETMPUpdate] =
    enrolment.split("~") match {
      case Array(_, identifierType, identifier) =>
        Right(ItsaETMPUpdate(identifierType, identifier, status))
      case _ => Left("Invalid enrolment")
    }

  def substituteMTDITIDValue: Either[String, ItsaETMPUpdate] =
    enrolment.split("~") match {
      case Array(_, "MTDITID", identifier) =>
        Right(ItsaETMPUpdate("MTDBSA", identifier, status))
      case _ => Left("Invalid enrolment")
    }
}

case class ItsaETMPUpdate(identifierType: String, identifier: String, status: Boolean)

object StatusUpdate {
  implicit val reads = Json.reads[StatusUpdate]
}
