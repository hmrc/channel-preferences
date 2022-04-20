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

import play.api.libs.json.{ Format, Json }

sealed trait Language {
  val name: String
}

case object EnglishLanguage extends Language {
  override val name: String = "en"
}

case object WelshLanguage extends Language {
  override val name: String = "cy"
}

object Language {
  implicit val englishLanguageFormat: Format[EnglishLanguage.type] = objectJsonFormat(EnglishLanguage)
  implicit val welshLanguageFormat: Format[WelshLanguage.type] = objectJsonFormat(WelshLanguage)
  implicit val languageFormat: Format[Language] = Json.format[Language]
}
