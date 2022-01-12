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

package uk.gov.hmrc.channelpreferences

import org.joda.time.DateTime
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.Helpers.{ route, _ }
import play.api.test._
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, EmailVerification }
import uk.gov.hmrc.channelpreferences.services.cds.CdsPreference
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.{ ExecutionContext, Future }

class ChannelBinderSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  private val appBuilder = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false)
    .overrides(bind[CdsPreference].to[MockCdsPreference])
    .build()

  "ChannelBinder" must {
    "define the bind - success" in {
      val request = FakeRequest(
        GET,
        "/channel-preferences/preference/email?enrolmentKey=HMRC-CUS-ORG&taxIdName=EORINumber&taxIdValue=1234567890")
      val test = route(appBuilder, request).get
      status(test) mustBe OK
    }

    "define the bind - failure" in {
      val request = FakeRequest(
        GET,
        "/channel-preferences/preference/Email?enrolmentKey=HMRC-CUS-ORG&taxIdName=EORINumber&taxIdValue=1234567890")
      val test = route(appBuilder, request).get
      status(test) mustBe BAD_REQUEST
    }

    "define the unbind" in {
      val test: Call = controllers.routes.PreferenceController.preference(Email, "test", "test", "123")
      test.url mustBe "/channel-preferences/preference/email?enrolmentKey=test&taxIdName=test&taxIdValue=123"
    }
  }
}

class MockCdsPreference extends CdsPreference {
  override def getPreference(c: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[Port, EmailVerification]] =
    Future.successful(Right(EmailVerification(EmailAddress("test@email.com"), new DateTime(2022, 3, 20, 1, 1))))
}
