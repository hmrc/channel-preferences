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

import play.api.libs.json.Json

import java.time.LocalDateTime
import java.util.UUID

trait TestModels {

  val timestamp = LocalDateTime.of(1987, 3, 20, 14, 33, 48, 640000);
  val keyIdentifier = "61ea7c5951d7a42da4fd4608";
  val managementId = UUID.randomUUID()
  val version = Version(1, 1, 1)
  val purposes = List(Purpose.one, Purpose.two)
  val message = Message(
    language = Language.en,
    nudge = true,
    archive = "3.months"
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
  val contextId = UUID.randomUUID()
  val verificationId = UUID.randomUUID()
  val confirmId = UUID.randomUUID()
  val verification = Verification(id = verificationId, sent = timestamp, email = "test@test.com")
  val confirm = Confirm(id = confirmId, started = timestamp)
  val consented =
    Consented(consentType = "default", status = true, created = timestamp, version = version, purposes = purposes)
  val context = Context(
    consented = consented,
    verification = verification,
    confirm = confirm
  )
  val contextPayload = ContextPayload(
    key = keyIdentifier,
    resourcePath = "email[index=primary]",
    expiry = timestamp,
    context = context
  )

  val contextJson = Json.parse(s"""
                                  |{
                                  | "id":"$contextId",
                                  | "key":"$keyIdentifier",
                                  | "resourcePath":"email[index=primary]",
                                  | "expiry":"$timestamp",
                                  | "context":{
                                  |   "consented":{
                                  |     "consentType":"default",
                                  |     "status":true,
                                  |     "created":"$timestamp",
                                  |     "version":{
                                  |       "major":1,
                                  |       "minor":1,
                                  |       "patch":1
                                  |     },
                                  |     "purposes":[
                                  |       "one",
                                  |       "two"
                                  |     ]
                                  |  },
                                  |  "verification":{
                                  |     "id":"$verificationId",
                                  |     "email":"test@test.com",
                                  |     "sent":"$timestamp"
                                  |  },
                                  |  "confirm":{
                                  |      "id":"$confirmId",
                                  |      "started":"$timestamp"
                                  |  }
                                  | }
                                  |}""".stripMargin)

  val managementJson = Json.parse(s"""
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
                                     |  "archive":"3.months"
                                     | },
                                     | "status":"ACTIVE"
                                     |}""".stripMargin)

}
