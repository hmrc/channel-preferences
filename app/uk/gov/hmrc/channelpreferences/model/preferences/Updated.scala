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

import play.api.libs.json._

import java.time.{ LocalDateTime }

case class Updated(value: LocalDateTime) extends AnyVal

object Updated {
  implicit val format: OFormat[Updated] = Json.format[Updated]
}

//object UpdatedMongo {
//
//  def convert(s: String): String = {
//    val instant = Instant.parse(s)
//    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London")).toString
//  }
//
////  val instantWrites: Writes[Instant] =
////    Writes
////      .at[String](__ \ "$date")
////      .contramap(_.toEpochMilli.toString)
//
//  val instantReadsMongo: Reads[Instant] =
//    Reads
//      .at[String](__ \ "$date" \ "$numberLong")
//      .orElse(Reads.at[String](JsPath).map(x => Instant.parse(x).toEpochMilli.toString))
//      .map(s => Instant.ofEpochMilli(s.toLong))
//
//  implicit val format: Format[Updated] = Json.valueFormat[Updated]
//}
//object UpdatedMongo {
//  implicit val instantWrites: Writes[Instant] =
//    Writes
//      .at[String](__ \ "$date" \ "$numberLong")
//      .contramap(_.toEpochMilli.toString)
//
//  implicit val instantReads: Reads[Instant] =
//    Reads
//      .at[String](__ \ "$date" \ "$numberLong")
//      .map(s => Instant.ofEpochMilli(s.toLong))
//
//  val format: Format[Updated] = Json.valueFormat[Updated]
//}
