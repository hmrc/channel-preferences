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

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, JsError, JsResult, JsSuccess, JsValue, Json, OFormat }

case class Preference(
  id: PreferenceId,
  enrolment: Enrolment,
  created: Created,
  consents: NonEmptyList[Consent],
  emailPreferences: List[EmailPreference]
)

object Preference {
  implicit def nonEmptyListFormat[A: Format]: Format[NonEmptyList[A]] = new Format[NonEmptyList[A]] {
    override def writes(o: NonEmptyList[A]): JsValue = Json.toJson(o.toList)

    override def reads(json: JsValue): JsResult[NonEmptyList[A]] =
      Json.fromJson[List[A]](json).flatMap { values =>
        NonEmptyList
          .fromList(values)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"non-empty list cannot be empty"))
      }
  }
  implicit val format: OFormat[Preference] = Json.format[Preference]
}
