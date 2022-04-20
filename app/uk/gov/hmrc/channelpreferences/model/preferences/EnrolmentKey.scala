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
import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait EnrolmentKey {
  val value: String
}

case object CustomsServiceKey extends EnrolmentKey {
  override val value: String = "HMRC-CUS-ORG"
}

object EnrolmentKey {
  implicit def enrolmentKeyBinder(implicit stringBinder: PathBindable[String]): PathBindable[EnrolmentKey] =
    new PathBindable[EnrolmentKey] {
      override def bind(key: String, value: String): Either[String, EnrolmentKey] =
        for {
          name <- stringBinder.bind(key, value).right
          ch   <- EnrolmentKey.fromValue(name)
        } yield ch

      override def unbind(key: String, enrolmentKey: EnrolmentKey): String = enrolmentKey.value
    }

  def fromValue(value: String): Either[String, EnrolmentKey] = value match {
    case CustomsServiceKey.value => CustomsServiceKey.asRight
    case other                   => s"EnrolmentKey: $other, not found".asLeft
  }

  implicit val customsServiceKeyFormat: Format[CustomsServiceKey.type] = objectJsonFormat(CustomsServiceKey)

  implicit val format: Format[EnrolmentKey] = Json.format[EnrolmentKey]
}
