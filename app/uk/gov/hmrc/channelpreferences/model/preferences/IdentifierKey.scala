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

import cats.syntax.either._
import play.api.mvc.PathBindable

sealed trait IdentifierKey {
  val value: String
}

object IdentifierKey {
  implicit def identifierKeyBinder(implicit stringBinder: PathBindable[String]): PathBindable[IdentifierKey] =
    new PathBindable[IdentifierKey] {
      override def bind(key: String, value: String): Either[String, IdentifierKey] =
        for {
          name <- stringBinder.bind(key, value)
          ch   <- IdentifierKey.fromValue(name)
        } yield ch

      override def unbind(key: String, IdentifierKey: IdentifierKey): String = IdentifierKey.value
    }

  def fromValue(value: String): Either[String, IdentifierKey] = value match {
    case EORINumber.value => EORINumber.asRight
    case other            => s"IdentifierKey: $other, not found".asLeft
  }

  case object EORINumber extends IdentifierKey {
    override val value: String = "EORINumber"
  }
}
