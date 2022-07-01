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
import play.api.libs.json.{ Format, JsError, JsResult, JsString, JsSuccess, JsValue }
import play.api.mvc.PathBindable

sealed trait Index {
  val name: String
}

case object PrimaryIndex extends Index {
  override val name: String = "Primary"
}

object Index {
  implicit object Format extends Format[Index] {
    override def writes(o: Index): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[Index] = json match {
      case JsString(name) =>
        name match {
          case PrimaryIndex.name => JsSuccess(PrimaryIndex)
          case other             => JsError(s"unsupported index type $other")
        }
      case other => JsError(s"expected a json string for index but got $other")
    }
  }

  implicit def indexBinder(implicit stringBinder: PathBindable[String]): PathBindable[Index] =
    new PathBindable[Index] {
      override def bind(key: String, value: String): Either[String, Index] =
        for {
          name <- stringBinder.bind(key, value).right
          ch   <- Index.fromValue(name)
        } yield ch

      override def unbind(key: String, index: Index): String = index.name
    }

  def fromValue(value: String): Either[String, Index] = value match {
    case PrimaryIndex.name => PrimaryIndex.asRight
    case other             => s"Index: $other, not found".asLeft
  }
}
