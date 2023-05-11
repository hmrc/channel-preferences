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
      encryptString("f0ea9b13-82bf-4d1a-9619-bb1e2481f8d8") mustBe Left(
        "YL4AfL0xsXviYiQtibOWOZVqZlKLwsna7Xq4XsFOLjkdcBJd5KldTXzrL81IsrEZ")
    }

  }

  "decryptString" must {
    "Successfully parse a string " in {
      decryptString("YL4AfL0xsXviYiQtibOWOZVqZlKLwsna7Xq4XsFOLjkdcBJd5KldTXzrL81IsrEZ") mustBe Left(
        "f0ea9b13-82bf-4d1a-9619-bb1e2481f8d8")
    }

  }

}
