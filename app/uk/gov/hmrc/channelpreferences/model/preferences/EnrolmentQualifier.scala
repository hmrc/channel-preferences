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

import cats.Parallel.parProduct
import cats.syntax.either._
import play.api.libs.json.{ Format, JsError, JsResult, JsString, JsSuccess, JsValue }
import play.api.mvc.PathBindable

sealed trait EnrolmentQualifier {
  val enrolmentKey: EnrolmentKey
  val identifierKey: IdentifierKey
  def value: String = s"${enrolmentKey.value}${Enrolment.Separator}${identifierKey.value}"
}

case object PensionsAdministratorQualifier extends EnrolmentQualifier {
  override val enrolmentKey: EnrolmentKey = PensionsOnlineKey
  override val identifierKey: IdentifierKey = PensionsAdministrator
}

case object PensionsPractitionerQualifier extends EnrolmentQualifier {
  override val enrolmentKey: EnrolmentKey = PensionsSchemePractitionerKey
  override val identifierKey: IdentifierKey = PensionsPractitioner
}

case object CustomsServiceQualifier extends EnrolmentQualifier {
  override val enrolmentKey: EnrolmentKey = CustomsServiceKey
  override val identifierKey: IdentifierKey = EORINumber
}

object EnrolmentQualifier {
  implicit def enrolmentQualifierBinder(
    implicit stringBinder: PathBindable[String]): PathBindable[EnrolmentQualifier] =
    new PathBindable[EnrolmentQualifier] {
      override def bind(key: String, value: String): Either[String, EnrolmentQualifier] =
        for {
          name <- stringBinder.bind(key, value).right
          ch   <- EnrolmentQualifier.fromValue(name)
        } yield ch

      override def unbind(key: String, enrolmentQualifier: EnrolmentQualifier): String = enrolmentQualifier.value
    }

  def fromValues(enrolmentKey: String, identifierKey: String): Either[String, EnrolmentQualifier] =
    fromValue(s"$enrolmentKey${Enrolment.Separator}$identifierKey")

  def fromValue(value: String): Either[String, EnrolmentQualifier] = value.split(Enrolment.Separator) match {
    case Array(enrolmentKey, identifierKey) =>
      parProduct(
        EnrolmentKey.fromValue(enrolmentKey),
        IdentifierKey.fromValue(identifierKey)
      ).flatMap {
        case (PensionsSchemePractitionerKey, PensionsPractitioner) => PensionsPractitionerQualifier.asRight
        case (PensionsOnlineKey, PensionsAdministrator)            => PensionsAdministratorQualifier.asRight
        case (CustomsServiceKey, EORINumber)                       => CustomsServiceQualifier.asRight
        case (otherKey, otherId) =>
          s"expected a valid enrolment qualifier, but got ${otherKey.value}${Enrolment.Separator}${otherId.value}".asLeft
      }
    case otherShape =>
      s"Expected [enrolmentKey]${Enrolment.Separator}[identifierKey], but got ${otherShape.mkString(Enrolment.Separator)}".asLeft
  }

  implicit object Format extends Format[EnrolmentQualifier] {
    override def writes(o: EnrolmentQualifier): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[EnrolmentQualifier] = json match {
      case JsString(value) =>
        EnrolmentQualifier
          .fromValue(value)
          .fold(
            JsError(_),
            JsSuccess(_)
          )
      case other => JsError(s"expected a json string for enrolment key but got $other")
    }
  }
}
