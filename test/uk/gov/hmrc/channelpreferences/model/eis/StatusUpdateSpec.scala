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

package uk.gov.hmrc.channelpreferences.model.eis

import org.scalatestplus.play.PlaySpec

class StatusUpdateSpec extends PlaySpec {

  "StatusUpdate.getItsaETMPUpdate" must {

    "extract the etmp itsa update" in {
      val statusUpdate = StatusUpdate("MTD-IT~MTDITID~XMIT983509385093485", true)
      val expectedResult = ItsaETMPUpdate("MTDITID", "XMIT983509385093485", true)
      statusUpdate.getItsaETMPUpdate mustBe Right(expectedResult)
    }

    "fail the request if the MTDITID is invalid" in {
      val invalidItsaEnrolment = "MTD-IT~MTDBSA~XMIT983509385093485~additionalpart"
      val statusUpdate = StatusUpdate(invalidItsaEnrolment, true)
      statusUpdate.getItsaETMPUpdate mustBe Left("Invalid enrolment")
    }

  }
  "StatusUpdate.substituteMTDITIDValue" must {

    "replace MTDTITID identifier in itsa enrolment" in {
      val statusUpdate = StatusUpdate("MTD-IT~MTDITID~XMIT983509385093485", true)
      val expectedResult = ItsaETMPUpdate("MTDBSA", "XMIT983509385093485", true)
      statusUpdate.substituteMTDITIDValue mustBe Right(expectedResult)
    }

    "fail the request if the MTDITID is not present" in {
      val statusUpdate = StatusUpdate("MTD-IT~MTDBSA~XMIT983509385093485", true)
      statusUpdate.substituteMTDITIDValue mustBe Left("Invalid enrolment")
    }

    "fail the request if the MTDITID is invalid" in {
      val invalidItsaEnrolment = "MTD-IT~MTDBSA~XMIT983509385093485~additionalpart"
      val statusUpdate = StatusUpdate(invalidItsaEnrolment, true)
      statusUpdate.substituteMTDITIDValue mustBe Left("Invalid enrolment")
    }

  }

}
