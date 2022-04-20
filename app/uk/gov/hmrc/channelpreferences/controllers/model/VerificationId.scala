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
import play.api.libs.json.{ Json, OFormat }
import play.api.mvc.PathBindable

import java.util.UUID

case class VerificationId(id: UUID) extends AnyVal

object VerificationId {
  implicit val format: OFormat[VerificationId] = Json.format[VerificationId]

  implicit def verificationIdBinder(implicit stringBinder: PathBindable[String]): PathBindable[VerificationId] =
    new PathBindable[VerificationId] {
      override def bind(key: String, value: String): Either[String, VerificationId] =
        for {
          name <- stringBinder.bind(key, value).right
          ch   <- VerificationId.fromValue(name)
        } yield ch

      override def unbind(key: String, verificationId: VerificationId): String = verificationId.id.toString
    }

  private def fromValue(value: String): Either[String, VerificationId] =
    Either
      .catchNonFatal(UUID.fromString(value))
      .fold(
        _.getMessage.asLeft,
        uuid => VerificationId(uuid).asRight
      )
}
