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

package uk.gov.hmrc.channelpreferences.model.cds

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsError, JsSuccess, Json }
import uk.gov.hmrc.emailaddress.EmailAddress

class EmailVerificationTest extends PlaySpec {

  private val dateTime = new DateTime(1987, 3, 20, 1, 2, 3)
  private val emailVerificationJson =
    Json.parse("""{"address":"some@email.com","timestamp":"1987-03-20T01:02:03.000Z"}""")

  private val invalidMmailVerificationJson =
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
      invalidMmailVerificationJson.validate[EmailVerification] mustBe a[JsError]
    }

  }

}
