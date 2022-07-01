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

package uk.gov.hmrc.channelpreferences.controllers.model

import play.api.libs.json._
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.{ Enrolment, Index }

sealed trait ContextId {
  def value: String
}

case class EnrolmentContextId(
  enrolment: Enrolment
) extends ContextId {
  override def value: String = enrolment.value
}

object EnrolmentContextId {
  implicit val format: OFormat[EnrolmentContextId] = Json.format[EnrolmentContextId]
}

case class IndexedEnrolmentContextId(
  enrolment: Enrolment,
  channel: Channel,
  index: Index
) extends ContextId {
  override def value: String =
    s"${enrolment.value}${Enrolment.Separator}${channel.name}${Enrolment.Separator}${index.name}"
}

object IndexedEnrolmentContextId {
  implicit val format: OFormat[IndexedEnrolmentContextId] =
    Json.format[IndexedEnrolmentContextId]
}

object ContextId {
  implicit object Format extends Format[ContextId] {
    override def writes(o: ContextId): JsValue = o match {
      case e: EnrolmentContextId        => EnrolmentContextId.format.writes(e)
      case i: IndexedEnrolmentContextId => IndexedEnrolmentContextId.format.writes(i)
    }

    override def reads(json: JsValue): JsResult[ContextId] =
      IndexedEnrolmentContextId.format
        .reads(json)
        .orElse(
          EnrolmentContextId.format.reads(json)
        )
  }
}
