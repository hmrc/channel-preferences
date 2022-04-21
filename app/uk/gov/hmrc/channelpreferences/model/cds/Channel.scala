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

package uk.gov.hmrc.channelpreferences.model.cds

import play.api.libs.json.{ Format, JsError, JsResult, JsString, JsSuccess, JsValue }

sealed abstract class Channel {
  val name: String
}

case object Email extends Channel { val name = "email" }
case object Phone extends Channel { val name = "phone" }
case object Sms extends Channel { val name = "sms" }
case object Paper extends Channel { val name = "paper" }

object Channel {
  def channelFromName(n: String): Either[String, Channel] = n match {
    case Email.name => Right[String, Channel](Email)
    case Phone.name => Right[String, Channel](Phone)
    case Sms.name   => Right[String, Channel](Sms)
    case Paper.name => Right[String, Channel](Paper)
    case _          => Left[String, Channel](s"Channel $n not found")
  }

  implicit object Format extends Format[Channel] {
    override def writes(o: Channel): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[Channel] = json match {
      case JsString(value) =>
        channelFromName(value).fold(
          JsError(_),
          JsSuccess(_)
        )
      case other => JsError(s"expected json string value for channel but got $other")
    }
  }
}
