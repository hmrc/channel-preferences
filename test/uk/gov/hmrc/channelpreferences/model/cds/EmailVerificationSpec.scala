/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.model.cds

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsError, JsSuccess, Json }
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress

import java.time.Instant

class EmailVerificationSpec extends PlaySpec {

  private val dateTime = Instant.parse("1987-03-20T01:02:03.000Z")
  private val emailVerificationJson =
    Json.parse("""{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}""")

  private val invalidEmailVerificationJson =
    Json.parse("""{"address":"email.com","timestamp":"1987-03-20T01:02:03.000Z"}""")

  private val emailVerification = EmailVerification(EmailAddress("some@email.com"), dateTime)

  "Validating an email verification" must {

    "be successful when the address and date are valid" in {
      emailVerificationJson.validate[EmailVerification] mustBe JsSuccess(emailVerification)
    }

    "be able to convert the email verification to Json" in {
      Json.toJson(emailVerification) mustBe emailVerificationJson
    }

    "be unsuccessful if the email is invalid" in {
      invalidEmailVerificationJson.validate[EmailVerification] mustBe a[JsError]
    }

    "be unsuccessful if the email address json is invalid" in {
      Json
        .parse("""{"address": 100,"timestamp":"1987-03-20T01:02:03.000Z"}""")
        .validate[EmailVerification] mustBe a[JsError]
    }
  }

}
