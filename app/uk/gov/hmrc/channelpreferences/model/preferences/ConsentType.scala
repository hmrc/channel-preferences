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

sealed trait ConsentType {
  val name: String
}

case object DefaultConsentType extends ConsentType {
  override val name: String = "Default"
}

object ConsentType {
  implicit object Format extends Format[ConsentType] {
    override def writes(o: ConsentType): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[ConsentType] = json match {
      case JsString(name) =>
        name match {
          case DefaultConsentType.name => JsSuccess(DefaultConsentType)
          case other                   => JsError(s"unsupported consent type $other")
        }
      case other => JsError(s"expected a json string for consent type but got $other")
    }
  }
}
