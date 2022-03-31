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

class ContextSpec extends PlaySpec {
  private val dateTime = new LocalDateTime(1987, 3, 20, 1, 2, 3)
  val verificationId = UUID.randomUUID()
  val keyId = UUID.randomUUID()
  val timestamp = dateTime
  val expiry = dateTime
  val verification = Verification(id = verificationId, sent = timestamp, email = "test@test.com")
  val version = Version(1, 1, 1)
  val confirm = Confirm(id = keyId, started = timestamp)
  val consented = ContextConsent(
    consentType = "not sure",
    status = true,
    created = timestamp,
    version = version,
    purposes = List(Purpose.one, Purpose.two))
  val context = Context(
    id = verificationId,
    key = "61ea7c5951d7a42da4fd4608",
    resourcePath = "email[index=primary]",
    expiry = expiry,
    consented = consented,
    verification = verification,
    confirm = confirm
  )
  val contextJson = Json.parse(s"""
                                  |{
                                  | "id":"$verificationId",
                                  | "key":"61ea7c5951d7a42da4fd4608",
                                  | "resourcePath":"email[index=primary]",
                                  | "expiry":"$expiry",
                                  | "consented":{
                                  |  "consentType":"not sure",
                                  |  "status" "true",
                                  |  "created":"$timestamp",
                                  |  "version":{
                                  |    "major":1,
                                  |    "minor":1,
                                  |    "patch":1
                                  |  },
                                  |  "purposes":[
                                  |    "one",
                                  |    "two"
                                  |  ]
                                  | },
                                  | "verification":{
                                  |  "id":"$verificationId",
                                  |  "email":"test@test.com",
                                  |  "sent":"$timestamp"
                                  | },
                                  | "keys":[
                                  |   {
                                  |     "id":"$keyId",
                                  |     "started:"$timestamp"
                                  |   }
                                  | ]
                                  |}
                                  |""".stripMargin)

  "read" must {
    "successfully parse from json" in {
      contextJson.validate[Context] mustBe JsSuccess(context)
    }
  }
}
