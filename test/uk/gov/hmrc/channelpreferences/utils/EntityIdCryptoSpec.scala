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

package uk.gov.hmrc.channelpreferences.utils

import org.scalatestplus.play.PlaySpec

class EntityIdCryptoSpec extends PlaySpec with EntityIdCrypto {

  "encryptString" must {
    "Successfully parse a string " in {
      encryptString("929412ba-9915-4a8b-ad88-9d15d4879b62") mustBe Left(
        "DtPae4zXATc9ja%2BGM3UEKrCUzv0O%2FviemRBEUJU0bb3%2FQu7y%2FdYBs5ZV8SA3hiWw")
    }

  }

  "decryptString" must {
    "Successfully parse a string " in {
      decryptString("DtPae4zXATc9ja%2BGM3UEKrCUzv0O%2FviemRBEUJU0bb3%2FQu7y%2FdYBs5ZV8SA3hiWw") mustBe Left(
        "929412ba-9915-4a8b-ad88-9d15d4879b62")
    }

  }

}
