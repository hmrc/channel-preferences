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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsSuccess, Json }

import java.time.LocalDateTime
import java.util.UUID

class EventSpec extends PlaySpec {

  val eventId = UUID.randomUUID()
  val timestamp = LocalDateTime.now()
  val event = Event(eventId, "subject", "groupId", timestamp, Json.parse("{}"))
  val eventJson = Json.parse(s"""
                                |{
                                |"eventId":"$eventId",
                                |"subject":"subject",
                                |"groupId":"groupId",
                                |"timestamp":"$timestamp",
                                |"event":{}
                                |}
                                |""".stripMargin)

  "read" must {
    "successfully parse from json" in {
      eventJson.validate[Event] mustBe JsSuccess(event)
    }
  }

  "write" must {
    "successfully parse into json" in {
      Json.toJson(event) mustBe eventJson
    }
  }
}
