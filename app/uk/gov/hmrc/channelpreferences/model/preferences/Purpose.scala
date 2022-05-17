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

import play.api.libs.json.{ Format, JsError, JsResult, JsString, JsSuccess, JsValue }

sealed trait Purpose {
  val name: String
}

case object DigitalCommunicationsPurpose extends Purpose {
  override val name: String = "DigitalCommunications"
}

object Purpose {
  implicit object Format extends Format[Purpose] {
    override def writes(o: Purpose): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[Purpose] = json match {
      case JsString(name) =>
        name match {
          case DigitalCommunicationsPurpose.name => JsSuccess(DigitalCommunicationsPurpose)
          case other                             => JsError(s"unsupported purpose $other")
        }
      case other => JsError(s"expected a json string for purpose but got $other")
    }
  }
}
