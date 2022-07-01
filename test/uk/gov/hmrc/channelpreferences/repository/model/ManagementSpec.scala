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

package uk.gov.hmrc.channelpreferences.repository.model

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsSuccess, Json }

import java.time.LocalDateTime
import java.util.UUID

class ManagementSpec extends PlaySpec {
  private val timestamp = LocalDateTime.of(1987, 3, 20, 14, 33, 48, 640000);
  val managementId = UUID.randomUUID()

  val version = Version(1, 1, 1)
  val purposes = List(Purpose.one, Purpose.two)
  val message = Message(
    language = Language.en,
    nudge = true,
    archive = "123"
  )
  val email = Email(
    index = EmailIndex.primary,
    email = "test@test.com",
    contentType = "text/plain",
    language = Language.en,
    contactable = true,
    purposes = purposes
  )
  val managementConsent = ManagementConsent(
    consentType = "default",
    status = true,
    updated = timestamp,
    version = version,
    purposes = purposes
  )

  val management = Management(
    id = managementId,
    key = List("HMRC-MTD-VAT~VRN~GB123456789", "HMRC-CUS-ORG~EORINumber~GB123456789"),
    created = timestamp,
    consent = List(managementConsent),
    email = List(email),
    message = message,
    status = Status.ACTIVE
  )

  val contextJson = Json.parse(s"""
                                  |{
                                  | "id":"$managementId",
                                  | "key":[
                                  |  "HMRC-MTD-VAT~VRN~GB123456789",
                                  |  "HMRC-CUS-ORG~EORINumber~GB123456789"
                                  | ],
                                  | "created":"$timestamp",
                                  | "consent":[
                                  |  {
                                  |    "consentType":"default",
                                  |    "status":true,
                                  |    "updated":"$timestamp",
                                  |     "version":{
                                  |       "major":1,
                                  |       "minor":1,
                                  |       "patch":1
                                  |     },
                                  |          "purposes":[
                                  |       "one",
                                  |       "two"
                                  |     ]
                                  |  }
                                  | ],
                                  | "email":[
                                  |  {
                                  |    "index":"primary",
                                  |    "email":"test@test.com",
                                  |    "contentType":"text/plain",
                                  |    "language":"en",
                                  |    "contactable":true,
                                  |     "purposes":[
                                  |       "one",
                                  |       "two"
                                  |     ]
                                  |  }
                                  | ],
                                  | "message":{
                                  |  "language":"en",
                                  |  "nudge":true,
                                  |  "archive":"123"
                                  | },
                                  | "status":"ACTIVE"
                                  |}""".stripMargin)

  "read" must {
    "successfully parse from json" in {
      contextJson.validate[Management] mustBe JsSuccess(management)
    }
  }
}
