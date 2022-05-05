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

package uk.gov.hmrc.channelpreferences.controllers.model

import cats.data.NonEmptyList
import org.scalatest.EitherValues
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.channelpreferences.model.preferences._

import java.time.{ LocalDateTime }
import java.util.UUID

trait TestModels extends EitherValues {

  val timestamp: LocalDateTime = LocalDateTime.of(1987, 3, 20, 14, 33, 48, 640000);
  val version: Version = Version(1, 1, 1)
  val purposes = List(DigitalCommunicationsPurpose)
  val enrolmentValue = "HMRC-CUS-ORG~EORINumber~GB123456789"
  val email: EmailPreference = EmailPreference(
    index = PrimaryIndex,
    email = EmailAddress("test@test.com"),
    contentType = TextPlain,
    language = EnglishLanguage,
    contactable = Contactable(true),
    purposes = purposes
  )
  val managementConsent: Consent = Consent(
    consentType = DefaultConsentType,
    status = ConsentStatus(true),
    updated = Updated(timestamp),
    version = version,
    purposes = purposes
  )

  val preference: Preference = Preference(
    enrolments = NonEmptyList.of(Enrolment.fromValue(enrolmentValue).right.value),
    created = Created(timestamp),
    consents = NonEmptyList.of(managementConsent),
    emailPreferences = List(email),
    status = Active
  )

  val contextPayload: ContextPayload = ContextPayload(
    EnrolmentContextId(Enrolment.fromValue(enrolmentValue).right.value),
    timestamp,
    managementConsent
  )

  val verificationId: VerificationId = VerificationId(UUID.randomUUID())

  val contextJson: JsValue = Json.parse("""
                                          |{
                                          |  "contextId" : {
                                          |    "enrolment" : "HMRC-CUS-ORG~EORINumber~GB123456789"
                                          |  },
                                          |  "expiry" : "1987-03-20T14:33:48.00064",
                                          |  "context" : {
                                          |    "consentType" : "Default",
                                          |    "status" : true,
                                          |    "updated" : "1987-03-20T14:33:48.000640Z",
                                          |    "version" : {
                                          |      "major" : 1,
                                          |      "minor" : 1,
                                          |      "patch" : 1
                                          |    },
                                          |    "purposes" : [ "DigitalCommunications" ]
                                          |  }
                                          |}
                                          |""".stripMargin)

  val preferenceJson: JsValue = Json.parse(s"""
                                              |{
                                              |  "enrolments" : [ "HMRC-CUS-ORG~EORINumber~GB123456789" ],
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
                                              |    "index" : "Primary",
                                              |    "email" : "test@test.com",
                                              |    "contentType" : "text/plain",
                                              |    "language" : "en",
                                              |    "contactable" : true,
                                              |    "purposes" : [ "DigitalCommunications" ]
                                              |  } ],
                                              |  "status" : "Active"
                                              |}
                                              |""".stripMargin)

}
