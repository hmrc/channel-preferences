/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.play.PlaySpec

class PreferenceRequestSpec extends PlaySpec {

  "PreferenceRequest" must {
    "deserialize from JSON correctly" in {
      val jsonString =
        """
          |{
          |  "enrolmentKey": "HMRC-CUS-ORG",
          |  "identifierKey": "EORINumber",
          |  "identifierValue": "GB1234567890",
          |  "channel": "email"
          |}
          |""".stripMargin

      val json = play.api.libs.json.Json.parse(jsonString)

      val result = json.validate[PreferenceRequest]

      result.isSuccess mustBe true

      val preferenceRequest = result.get

      preferenceRequest.enrolmentKey mustBe EnrolmentKey.CustomsServiceKey
      preferenceRequest.identifierKey mustBe IdentifierKey.EORINumber
      preferenceRequest.identifierValue.value mustBe "GB1234567890"
      preferenceRequest.channel mustBe uk.gov.hmrc.channelpreferences.model.cds.Email
    }
  }

}
