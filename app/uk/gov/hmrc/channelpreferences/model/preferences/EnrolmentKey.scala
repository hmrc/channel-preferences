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

case object PensionsOnlineKey extends EnrolmentKey {
  override val value: String = "HMRC-PODS-ORG"
}

case object PensionsSchemePractitionerKey extends EnrolmentKey {
  override val value: String = "HMRC-PODSPP-ORG"
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
    case CustomsServiceKey.value             => CustomsServiceKey.asRight
    case PensionsOnlineKey.value             => PensionsOnlineKey.asRight
    case PensionsSchemePractitionerKey.value => PensionsSchemePractitionerKey.asRight
    case other                               => s"EnrolmentKey: $other, not found".asLeft
  }

  implicit object Format extends Format[EnrolmentKey] {
    override def writes(o: EnrolmentKey): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[EnrolmentKey] = json match {
      case JsString(value) =>
        EnrolmentKey
          .fromValue(value)
          .fold(
            JsError(_),
            JsSuccess(_)
          )
      case other => JsError(s"expected a json string for enrolment key but got $other")
    }
  }
}
