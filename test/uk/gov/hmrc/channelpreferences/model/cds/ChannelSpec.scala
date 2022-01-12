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

package uk.gov.hmrc.channelpreferences.model.cds

import org.scalatestplus.play.PlaySpec

class ChannelSpec extends PlaySpec {

  "Channel.channelFromName" must {
    "return the correct Channel type for given name" in {
      Channel.channelFromName("email") mustBe Right(Email)
      Channel.channelFromName("phone") mustBe Right(Phone)
      Channel.channelFromName("sms") mustBe Right(Sms)
      Channel.channelFromName("paper") mustBe Right(Paper)
    }

    "return the error when channel name is not found" in {
      Channel.channelFromName("xyz") mustBe Left("Channel xyz not found")
    }
  }

}
