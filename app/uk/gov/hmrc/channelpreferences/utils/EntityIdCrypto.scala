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

import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.crypto.{ Crypted, CryptoWithKeysFromConfig, PlainText }

trait EntityIdCrypto {
  lazy val currentCrypto = new CryptoWithKeysFromConfig(baseConfigKey = "entityId.encryption", ConfigFactory.load())

  def encryptString(s: String): Either[String, EncryptOrDecryptException] =
    try Left(currentCrypto.encrypt(PlainText(s)).value)
    catch {
      case e: Throwable => Right(EncryptOrDecryptException(e.getMessage))
    }

  def decryptString(s: String): Either[String, EncryptOrDecryptException] =
    try Left(currentCrypto.decrypt(Crypted(s)).value)
    catch {
      case e: Throwable => Right(EncryptOrDecryptException(e.getMessage))
    }

  case class EncryptOrDecryptException(message: String)
}