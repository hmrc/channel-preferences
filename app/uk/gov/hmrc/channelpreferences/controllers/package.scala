/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences

import akka.util.ByteString
import play.api.http.{ ContentTypes, HttpEntity }
import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Results.Ok
import play.api.mvc.{ ResponseHeader, Result }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError

package object controllers {
  def toResult[T: Writes](eitherResult: Either[PreferenceError, T]): Result = eitherResult.fold(
    preferenceError =>
      Result(
        header = ResponseHeader(preferenceError.statusCode.intValue()),
        body = HttpEntity.Strict(ByteString.apply(preferenceError.message), Some(ContentTypes.TEXT))
    ),
    result => Ok(Json.toJson(result))
  )
}
