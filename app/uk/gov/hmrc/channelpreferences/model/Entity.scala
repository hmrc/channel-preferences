/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.model

import play.api.libs.json.{ Json, OFormat }

/**
  * It represents a taxable entity whose tax identifiers are retrievable from the entity-resolver service
  *
  * @param _id is the unique identifier, normally used as lookup key
  * @param nino is the (maybe) National Insurance Number
  * @param saUtr is the (maybe) Self Assessment Unique Tax Reference
  * @param itsa is the (maybe) Income Tax Self Assessment identifier
  */
final case class Entity(_id: String, saUtr: Option[String], nino: Option[String], itsa: Option[String])

object Entity {
  implicit val format: OFormat[Entity] = Json.format[Entity]
}
