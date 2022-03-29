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

package uk.gov.hmrc.channelpreferences.repository.model

import play.api.libs.json.{ Format, Reads, Writes }

import java.time.LocalDateTime

sealed trait Purpose

object Purpose extends Enumeration {
  type Purpose = Value
  val one, two, three = Value

  implicit val format: Format[Purpose] = Format(Reads.enumNameReads(Purpose), Writes.enumNameWrites)
}

sealed trait EmailIndex

object EmailIndex extends Enumeration {
  type EmailIndex = Value
  val primary, secondary, notification = Value

  implicit val format: Format[EmailIndex] = Format(Reads.enumNameReads(EmailIndex), Writes.enumNameWrites)
}

sealed trait Language

object Language extends Enumeration {
  type Language = Value
  val en, cy = Value

  implicit val format: Format[Language] = Format(Reads.enumNameReads(Language), Writes.enumNameWrites)
}

sealed trait Status

object Status extends Enumeration {
  type Status = Value
  val ACTIVE, PENDING, INACTIVE, SUSPENDED, ARCHIVED = Value

  implicit val format: Format[Status] = Format(Reads.enumNameReads(Status), Writes.enumNameWrites)
}

case class Message(
  language: Language,
  nudge: Boolean,
  archive: String
)

case class Email(
  index: EmailIndex,
  email: String,
  contentType: String,
  language: Language,
  contactable: Boolean,
  purposes: List[Purpose]
)

case class Version(
  major: Int,
  minor: Int,
  patch: Int
)

case class ManagementConsent(
  `type`: String,
  status: Boolean,
  updated: LocalDateTime,
  version: Version,
  purposes: List[Purpose]
)

case class Management(
  id: String,
  key: List[String],
  created: LocalDateTime,
  consent: List[ManagementConsent],
  email: List[Email],
  message: Message,
  status: Status
)
