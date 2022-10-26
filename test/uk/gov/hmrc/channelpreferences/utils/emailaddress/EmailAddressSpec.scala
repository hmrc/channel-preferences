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

package uk.gov.hmrc.channelpreferences.utils.emailaddress

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.matchers.should.Matchers.{ a, an, convertToAnyShouldWrapper, equal, thrownBy }
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress.{ Domain, Mailbox }

class EmailAddressSpec extends AnyFreeSpec with ScalaCheckPropertyChecks with EmailAddressGenerators {
  //with AnyFreeSpecLike with Matchers with ScalaCheckPropertyChecks with EmailAddressGenerators {

  "Creating an EmailAddress class" - {
    "work for a valid email" in {
      forAll(validEmailAddresses()) { address =>
        EmailAddress(address).value shouldBe (address)
      }
    }

    "throw an exception for an invalid email" in {
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress("sausages") }
    }

    "throw an exception for an valid email starting with invalid characters" in {
      forAll(validEmailAddresses()) { address =>
        an[IllegalArgumentException] shouldBe thrownBy { EmailAddress("§" + address) }
      }
    }

    "throw an exception for an valid email ending with invalid characters" in {
      forAll(validEmailAddresses()) { address =>
        an[IllegalArgumentException] shouldBe thrownBy { EmailAddress(address + "§") }
      }
    }

    "throw an exception for an empty email" in {
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress("") }
    }

    "throw an exception for a repeated email" in {
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress("test@domain.comtest@domain.com") }
    }

    "throw an exception when the '@' is missing" in {
      forAll { s: String =>
        whenever(!s.contains("@")) {
          an[IllegalArgumentException] shouldBe thrownBy { EmailAddress(s) }
        }
      }
    }
  }

  "An EmailAddress class" - {
    "implicitly convert to a String of the address" in {
      val e: String = EmailAddress("test@domain.com")
      e shouldBe ("test@domain.com")
    }
    "toString to a String of the address" in {
      val e = EmailAddress("test@domain.com")
      e.toString shouldBe ("test@domain.com")
    }
    "be obfuscatable" in {
      EmailAddress("abcdef@example.com").obfuscated.value should be("a****f@example.com")
    }
    "have a local part" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      val exampleAddr = EmailAddress(s"$mailbox@$domain")
      exampleAddr.mailbox should be(a[Mailbox])
      exampleAddr.domain should be(a[Domain])
    }
  }

  "A email address domain" - {
    "be extractable from an address" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      EmailAddress(s"$mailbox@$domain").domain should be(a[Domain])
    }
    "be creatable for a valid domain" in forAll(validDomain) { domain =>
      EmailAddress.Domain(domain) should be(a[Domain])
    }
    "not create for invalid domains" in {
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress.Domain("") }
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress.Domain("e.") }
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress.Domain(".uk") }
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress.Domain(".com") }
      an[IllegalArgumentException] shouldBe thrownBy { EmailAddress.Domain("*domain") }
    }
    "compare equal if identical" in forAll(validDomain, validMailbox, validMailbox) { (domain, mailboxA, mailboxB) =>
      val exampleA = EmailAddress(s"$mailboxA@$domain")
      val exampleB = EmailAddress(s"$mailboxB@$domain")
      exampleA.domain should equal(exampleB.domain)
    }
    "not compare equal if completely different" in forAll(validMailbox, validDomain, validDomain) {
      (mailbox, domainA, domainB) =>
        val exampleA = EmailAddress(s"$mailbox@$domainA")
        val exampleB = EmailAddress(s"$mailbox@$domainB")
        exampleA.domain should not equal exampleB.domain
    }
    "toString to a String of the domain" in {
      Domain("domain.com").toString should be("domain.com")
    }
    "implicitly convert to a String of the domain" in {
      val e: String = Domain("domain.com")
      e should be("domain.com")
    }
  }

  "A email address mailbox" - {

    "be extractable from an address" in forAll(validMailbox, validDomain) { (mailbox, domain) =>
      EmailAddress(s"$mailbox@$domain").mailbox should be(a[Mailbox])
    }
    "compare equal" in forAll(validMailbox, validDomain, validDomain) { (mailbox, domainA, domainB) =>
      val exampleA = EmailAddress(s"$mailbox@$domainA")
      val exampleB = EmailAddress(s"$mailbox@$domainB")
      exampleA.mailbox should equal(exampleB.mailbox)
    }
    "not compare equal if completely different" in forAll(validDomain, validMailbox, validMailbox) {
      (domain, mailboxA, mailboxB) =>
        val exampleA = EmailAddress(s"$mailboxA@$domain")
        val exampleB = EmailAddress(s"$mailboxB@$domain")
        exampleA.mailbox should not equal exampleB.mailbox
    }
    "toString to a String of the domain" in {
      EmailAddress("test@domain.com").mailbox.toString should be("test")
    }
    "implicitly convert to a String of the domain" in {
      val e: String = EmailAddress("test@domain.com").mailbox
      e should be("test")
    }
  }
}
