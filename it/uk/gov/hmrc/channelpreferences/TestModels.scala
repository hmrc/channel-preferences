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

package uk.gov.hmrc.channelpreferences

import cats.data.NonEmptyList
import org.scalatest.EitherValues
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ConsentContext, ContextPayload, EnrolmentContextId, Verification, VerificationId, Version }
import uk.gov.hmrc.channelpreferences.model.preferences._

import java.time.{ LocalDateTime, ZoneOffset }
import java.util.UUID

trait TestModels extends EitherValues {

  val timestamp: LocalDateTime = LocalDateTime.of(1987, 3, 20, 14, 33, 48, 640000)
  val version: Version = Version(1, 1, 1)
  val purposes = List(DigitalCommunicationsPurpose)
  val enrolmentValue = "HMRC-PODS-ORG~PSAID~GB123456789"
  val enrolment: Enrolment = Enrolment.fromValue(enrolmentValue).right.value
  val enrolments: NonEmptyList[Enrolment] = NonEmptyList.of(enrolment)
  val emailAddress: EmailAddress = EmailAddress("test@test.com")
  val email: EmailPreference = EmailPreference(
    index = PrimaryIndex,
    email = emailAddress,
    contentType = TextPlain,
    language = EnglishLanguage,
    contactable = Contactable(true),
    purposes = purposes
  )
  val updated: Updated = Updated(timestamp.toInstant(ZoneOffset.UTC))
  val consent: Consent = Consent(
    consentType = DefaultConsentType,
    status = ConsentStatus(true),
    updated = updated,
    version = version,
    purposes = purposes
  )

  val consentContext: ConsentContext = ConsentContext(consent, None)

  val preference: Preference = Preference(
    enrolments = enrolments,
    created = Created(timestamp.toInstant(ZoneOffset.UTC)),
    consents = NonEmptyList.of(consent),
    emailPreferences = List(email),
    status = Active
  )

  val contextPayload: ContextPayload = ContextPayload(
    EnrolmentContextId(enrolments),
    timestamp,
    consentContext
  )

  val verificationId: VerificationId = VerificationId(UUID.fromString("e273ce4e-c0b4-4189-8eca-ca6ab58744aa"))
  val verification: Verification = Verification(
    verificationId,
    emailAddress,
    timestamp
  )

  val contextJson: JsValue = Json.parse("""
                                          |{
                                          |  "contextId" : {
                                          |    "enrolments" : [ "HMRC-PODS-ORG~PSAID~GB123456789" ]
                                          |  },
                                          |  "expiry" : "1987-03-20T14:33:48.00064",
                                          |  "context" : {
                                          |   "consent" : {
                                          |      "consentType" : "Default",
                                          |      "status" : true,
                                          |      "updated" : "1987-03-20T14:33:48.000640Z",
                                          |      "version" : {
                                          |        "major" : 1,
                                          |        "minor" : 1,
                                          |        "patch" : 1
                                          |      },
                                          |      "purposes" : [ "DigitalCommunications" ]
                                          |    }
                                          | }
                                          |}
                                          |""".stripMargin)

  val preferenceJson: JsValue = Json.parse(s"""
                                              |{
                                              |  "enrolments" : [ "HMRC-PODS-ORG~PSAID~GB123456789" ],
                                              |  "created" : "1987-03-20T14:33:48.000640Z",
                                              |  "consents" : [ {
                                              |    "consentType" : "Default",
                                              |    "status" : true,
                                              |    "updated" : "1987-03-20T14:33:48.000640Z",
                                              |    "version" : {
                                              |      "major" : 1,
                                              |      "minor" : 1,
                                              |      "patch" : 1
                                              |    },
                                              |    "purposes" : [ "DigitalCommunications" ]
                                              |  } ],
                                              |  "emailPreferences" : [ {
                                              |    "index" : "primary",
                                              |    "email" : "test@test.com",
                                              |    "contentType" : "text/plain",
                                              |    "language" : "en",
                                              |    "contactable" : true,
                                              |    "purposes" : [ "DigitalCommunications" ]
                                              |  } ],
                                              |  "status" : "Active"
                                              |}
                                              |""".stripMargin)

  val consentJson: String = s"""
                               |{
                               |  "consentType": "Default",
                               |  "status": true,
                               |  "updated": "1987-03-20T14:33:48.000640Z",
                               |  "version": {
                               |    "major": 1,
                               |    "minor": 1,
                               |    "patch": 1
                               |  },
                               |  "purposes": [
                               |    "DigitalCommunications"
                               |  ]
                               |}
      """.stripMargin

  val emailJson: String = """{ "value": "test@test.com" }"""

  val jsonHeader: (String, String) = "Content-Type" -> "application/json"
}
