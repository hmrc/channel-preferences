/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.model.entityresolver

import play.api.libs.json.{ Json, Reads }

case class ItsaIdUpdateResponse(
  response: String,
  details: Option[String],
  error: Option[String],
  preference: Option[PreferenceResponse]
) {
  def isDigitalStatus: Boolean = {
    val isDigital = preference.map(_.digital).exists(identity)
    val isEmailVerified = preference.flatMap(_.email).map(_.isVerified).exists(identity)
    val hasBounces = preference.flatMap(_.email).map(_.hasBounces).exists(identity)
    isDigital && isEmailVerified && !hasBounces
  }
}

object ItsaIdUpdateResponse {
  implicit val reads: Reads[ItsaIdUpdateResponse] = Json.reads[ItsaIdUpdateResponse]
}
