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

package uk.gov.hmrc

import _root_.play.api.mvc.PathBindable
import uk.gov.hmrc.channelpreferences.model.cds.Channel

package object channelpreferences {

  object ChannelBinder {
    implicit def channelBinder(implicit stringBinder: PathBindable[String]): PathBindable[Channel] =
      new PathBindable[Channel] {
        override def bind(key: String, value: String): Either[String, Channel] =
          for {
            name <- stringBinder.bind(key, value).right
            ch   <- Channel.channelFromName(name)
          } yield ch

        override def unbind(key: String, value: Channel): String = value.name
      }
  }

}
