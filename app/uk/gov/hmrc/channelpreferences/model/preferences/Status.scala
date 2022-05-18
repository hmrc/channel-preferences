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

sealed trait Status {
  val name: String
}

case object Active extends Status {
  override val name = "Active"
}

case object Pending extends Status {
  override val name = "Pending"
}

case object Inactive extends Status {
  override val name = "Inactive"
}

case object Suspended extends Status {
  override val name = "Suspended"
}

case object Archived extends Status {
  override val name = "Archived"
}

object Status {
  implicit object Format extends Format[Status] {
    override def writes(o: Status): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[Status] = json match {
      case JsString(name) =>
        name match {
          case Active.name    => JsSuccess(Active)
          case Pending.name   => JsSuccess(Pending)
          case Inactive.name  => JsSuccess(Inactive)
          case Suspended.name => JsSuccess(Suspended)
          case Archived.name  => JsSuccess(Archived)
          case other          => JsError(s"unsupported status type $other")
        }
      case other => JsError(s"expected a json string for status but got $other")
    }
  }
}
