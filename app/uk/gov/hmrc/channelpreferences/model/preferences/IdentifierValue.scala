/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.syntax.either._
import play.api.libs.json._
import play.api.mvc.PathBindable

case class IdentifierValue(value: String) extends AnyVal

object IdentifierValue {
  implicit val reads: Reads[IdentifierValue] = Reads[IdentifierValue] { json =>
    json.validate[String].map(IdentifierValue(_))
  }

  implicit def identifierValueBinder(implicit stringBinder: PathBindable[String]): PathBindable[IdentifierValue] =
    new PathBindable[IdentifierValue] {
      override def bind(Value: String, value: String): Either[String, IdentifierValue] =
        for {
          name <- stringBinder.bind(Value, value)
          ch   <- IdentifierValue(name).asRight
        } yield ch

      override def unbind(Value: String, IdentifierValue: IdentifierValue): String = IdentifierValue.value
    }
}
