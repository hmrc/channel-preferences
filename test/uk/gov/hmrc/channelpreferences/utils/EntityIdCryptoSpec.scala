/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.channelpreferences.utils

import org.scalatestplus.play.PlaySpec

class EntityIdCryptoSpec extends PlaySpec with EntityIdCrypto {

  "encryptString" must {
    "Successfully parse a string " in {
      encryptString("someKey") mustBe Left("1YSXrdZ+2rKy7fDuGQ9DCA==")
    }

  }

  "decryptString" must {
    "Successfully parse a string " in {
      decryptString("1YSXrdZ+2rKy7fDuGQ9DCA==") mustBe Left("someKey")
    }

  }

}
