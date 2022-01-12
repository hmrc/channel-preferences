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

package uk.gov.hmrc.channelpreferences.model.entityresolver

import org.scalatestplus.play.PlaySpec

class EnrolmentResponseBodySpec extends PlaySpec {

  "Enrolment.isDigitalStatus" must {

    "be true when the preference is digital, has a verified email and no bounces" in {
      val enrolmentResponseBody =
        EnrolmentResponseBody(
          "Ok",
          Some(
            PreferenceResponse(
              Some(
                EmailPreference(
                  isVerified = true,
                  hasBounces = false
                )
              ),
              digital = true
            )
          )
        )

      enrolmentResponseBody.isDigitalStatus mustBe true
    }

    "be false when the email is not verified" in {
      val enrolmentResponseBody =
        EnrolmentResponseBody(
          "Ok",
          Some(
            PreferenceResponse(
              Some(
                EmailPreference(
                  isVerified = false,
                  hasBounces = false
                )
              ),
              digital = true
            )
          )
        )

      enrolmentResponseBody.isDigitalStatus mustBe false
    }

    "be false when the it has a bounce" in {
      val enrolmentResponseBody =
        EnrolmentResponseBody(
          "Ok",
          Some(
            PreferenceResponse(
              Some(
                EmailPreference(
                  isVerified = true,
                  hasBounces = true
                )
              ),
              digital = true
            )
          )
        )

      enrolmentResponseBody.isDigitalStatus mustBe false
    }

    "be false when the preference is NOT digital" in {
      val enrolmentResponseBody =
        EnrolmentResponseBody(
          "Ok",
          Some(
            PreferenceResponse(
              None,
              digital = false
            )
          )
        )

      enrolmentResponseBody.isDigitalStatus mustBe false
    }

  }

}
