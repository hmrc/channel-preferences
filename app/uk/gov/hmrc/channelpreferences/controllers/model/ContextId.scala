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

import cats.syntax.either._
import cats.syntax.parallel._
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.{ Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, JsonConfiguration, JsonNaming }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.ParseError
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelledEnrolment, Enrolment, Index }

sealed trait ContextId {
  def value: String
}

case class EnrolmentContextId(
  enrolment: Enrolment
) extends ContextId {
  override def value: String = enrolment.value
}

object EnrolmentContextId {
  implicit object EnrolmentContextIdFormat extends Format[EnrolmentContextId] {
    override def writes(o: EnrolmentContextId): JsValue = JsString(o.enrolment.value)

    override def reads(json: JsValue): JsResult[EnrolmentContextId] = json match {
      case JsString(value) =>
        Enrolment
          .fromValue(value)
          .fold(
            preferenceError => JsError(preferenceError.message),
            enrolment => JsSuccess(EnrolmentContextId(enrolment))
          )

      case otherJsValue => JsError(s"expected a json string for enrolment context id but got, $otherJsValue")
    }
  }
}

case class IndexedEnrolmentContextId(
  channelledEnrolment: ChannelledEnrolment,
  index: Index
) extends ContextId {
  override def value: String = IndexedEnrolmentContextId.value(this)
}

object IndexedEnrolmentContextId {
  def value(indexedEnrolmentContextId: IndexedEnrolmentContextId): String =
    s"${ChannelledEnrolment.value(indexedEnrolmentContextId.channelledEnrolment)}${Enrolment.Separator}${indexedEnrolmentContextId.index.name}"

  implicit object IndexedEnrolmentContextIdFormat extends Format[IndexedEnrolmentContextId] {
    override def writes(o: IndexedEnrolmentContextId): JsValue = JsString(value(o))

    override def reads(json: JsValue): JsResult[IndexedEnrolmentContextId] = json match {
      case JsString(value) =>
        value.split(Enrolment.Separator) match {
          case Array(enrolmentKey, identifierKey, identifierValue, channelValue, indexValue) =>
            val channelledEnrolment =
              ChannelledEnrolment.fromQuad(enrolmentKey, identifierKey, identifierValue, channelValue)
            val index = Index.fromValue(indexValue).leftMap(ParseError)

            (channelledEnrolment, index)
              .parMapN(IndexedEnrolmentContextId.apply)
              .fold(
                preferenceError => JsError(preferenceError.message),
                JsSuccess(_)
              )

          case anotherShape =>
            JsError(s"expected 5 values for a indexed channelled enrolment, but got ${anotherShape.mkString(" | ")}")
        }
      case otherJsValue =>
        JsError(s"expected a json string for indexed channelled enrolment context id but got, $otherJsValue")
    }
  }
}

object ContextId {
  implicit val jsonConfiguration: Aux[Json.MacroOptions] = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming(_.split("\\.").last)
  )
  implicit object Format extends Format[ContextId] {
    override def writes(o: ContextId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[ContextId] =
      IndexedEnrolmentContextId.IndexedEnrolmentContextIdFormat
        .reads(json)
        .orElse(
          EnrolmentContextId.EnrolmentContextIdFormat.reads(json)
        )
  }
}
