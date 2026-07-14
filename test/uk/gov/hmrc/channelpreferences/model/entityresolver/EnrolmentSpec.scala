/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.model.entityresolver

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentSpec extends PlaySpec {

  "Enrolment" should {

    "extract the identifier from a prefixed ITSA ID" in {
      val enrolment = Enrolment(
        entityId = "entity1",
        itsaId = "HMRC-MTD-IT~MTDITID~XAIT00000000015"
      )

      enrolment.itsaIdWithoutPrefix mustBe "XAIT00000000015"
    }

    "return the identifier when unprefixed ITSA ID passed" in {
      val enrolment = Enrolment(
        entityId = "entity1",
        itsaId = "XAIT00000000015"
      )

      enrolment.itsaIdWithoutPrefix mustBe "XAIT00000000015"
    }
  }

  "Enrolment JSON format" should {

    "serialize to JSON" in {
      val enrolment = Enrolment(
        entityId = "entity1",
        itsaId = "XAIT00000000015"
      )

      Json.toJson(enrolment) mustBe Json.obj(
        "entityId" -> "entity1",
        "itsaId"   -> "XAIT00000000015"
      )
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "entityId" -> "entity1",
        "itsaId"   -> "XAIT00000000015"
      )

      json.as[Enrolment] mustBe Enrolment(
        entityId = "entity1",
        itsaId = "XAIT00000000015"
      )
    }
  }
}
