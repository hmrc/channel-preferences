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

import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.channelpreferences.model.preferences.Preference

sealed trait ContextualPreference

case class PreferenceWithoutContext(
  preference: Preference
) extends ContextualPreference

object PreferenceWithoutContext {
  implicit val format: OFormat[PreferenceWithoutContext] = Json.format[PreferenceWithoutContext]
}

case class PreferenceContext(
  context: Context
) extends ContextualPreference

object PreferenceContext {
  implicit val format: OFormat[PreferenceContext] = Json.format[PreferenceContext]
}

case class PreferenceWithContext(
  preference: Preference,
  contexts: List[Context]
) extends ContextualPreference

object PreferenceWithContext {
  implicit val format: OFormat[PreferenceWithContext] = Json.format[PreferenceWithContext]
}

object ContextualPreference {
  implicit val format: OFormat[ContextualPreference] = Json.format[ContextualPreference]
}
