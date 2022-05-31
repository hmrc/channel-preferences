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

import cats.Order
import cats.syntax.either._
import cats.syntax.parallel._
import play.api.libs.json._
import play.api.mvc.PathBindable
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.ParseError
import uk.gov.hmrc.channelpreferences.services.preferences.PreferenceResolver

sealed trait Enrolment {
  val enrolmentQualifier: EnrolmentQualifier
  val identifierValue: IdentifierValue

  final def value: String = s"${enrolmentQualifier.value}${Enrolment.Separator}${identifierValue.value}"
}

case class CustomsServiceEnrolment(
  identifierValue: IdentifierValue
) extends Enrolment {
  override val enrolmentQualifier: EnrolmentQualifier = CustomsServiceQualifier
}

case class PensionsAdministratorEnrolment(
  identifierValue: IdentifierValue
) extends Enrolment {
  override val enrolmentQualifier: EnrolmentQualifier = PensionsAdministratorQualifier
}

case class PensionsPractitionerEnrolment(
  identifierValue: IdentifierValue
) extends Enrolment {
  override val enrolmentQualifier: EnrolmentQualifier = PensionsPractitionerQualifier
}

object CustomsServiceEnrolment {
  implicit val format: OFormat[CustomsServiceEnrolment] = Json.format[CustomsServiceEnrolment]
}

object Enrolment {
  implicit val enrolmentOrder: Order[Enrolment] = (x: Enrolment, y: Enrolment) => x.value.compareTo(y.value)
  implicit val enrolmentOrdering: Ordering[Enrolment] = enrolmentOrder.toOrdering

  val Separator = "~"

  implicit object Format extends Format[Enrolment] {
    override def writes(o: Enrolment): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[Enrolment] = json match {
      case JsString(value) =>
        fromValue(value).fold(
          preferencesError => JsError(preferencesError.message),
          JsSuccess(_)
        )
      case json => JsError(s"expected a json string value for Enrolment, but got $json")
    }
  }

  def fromValue(value: String): Either[PreferenceError, Enrolment] =
    value.split(Separator) match {
      case Array(enrolmentKey, identifierKey, identifierValue) =>
        fromTriplet(enrolmentKey, identifierKey, identifierValue)
      case anotherShape =>
        ParseError(s"expected 3 values for an enrolment, but got ${anotherShape.mkString(" | ")}").asLeft
    }

  def fromTriplet(
    enrolmentKey: String,
    identifierKey: String,
    identifierValue: String): Either[PreferenceError, Enrolment] = {
    val tupled = (
      EnrolmentQualifier
        .fromValue(s"$enrolmentKey${Enrolment.Separator}$identifierKey")
        .leftMap[PreferenceError](ParseError),
      IdentifierValue(identifierValue).asRight[PreferenceError]
    ).parTupled

    tupled.flatMap {
      case (enrolmentQualifier, identifierValue) =>
        PreferenceResolver.toEnrolment(enrolmentQualifier, identifierValue)
    }
  }

  implicit def enrolmentBinder(implicit stringBinder: PathBindable[String]): PathBindable[Enrolment] =
    new PathBindable[Enrolment] {
      override def bind(key: String, value: String): Either[String, Enrolment] =
        for {
          name <- stringBinder.bind(key, value).right
          ch   <- Enrolment.fromValue(name).leftMap(_.message)
        } yield ch

      override def unbind(key: String, enrolment: Enrolment): String = enrolment.value
    }
}
