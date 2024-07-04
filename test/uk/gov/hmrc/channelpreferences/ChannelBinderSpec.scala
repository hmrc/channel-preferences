/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.syntax.either.*
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Call
import play.api.test.Helpers.*
import play.api.test.{ FakeRequest, Injecting }
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, EmailVerification }
import uk.gov.hmrc.channelpreferences.model.preferences.EnrolmentKey.CustomsServiceKey
import uk.gov.hmrc.channelpreferences.model.preferences.IdentifierKey.EORINumber
import uk.gov.hmrc.channelpreferences.model.preferences.{ EnrolmentKey, IdentifierKey, IdentifierValue, PreferenceError }
import uk.gov.hmrc.channelpreferences.services.preferences.{ PreferenceResolver, PreferenceService }
import uk.gov.hmrc.channelpreferences.utils.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.GET

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class ChannelBinderSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false, "auditing.enabled" -> false)
      .overrides(bind[PreferenceService].to[PreferenceServiceMock])
      .build()

  "ChannelBinder" must {

    "define the bind - success" in {
      val request = FakeRequest(
        GET,
        "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/email"
      )
      val test = route(fakeApplication(), request).get
      status(test) mustBe OK
    }

    "define the bind - failure" in {
      val request = FakeRequest(
        GET,
        "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/badChannel"
      )
      val test = route(fakeApplication(), request).get
      status(test) mustBe BAD_REQUEST
    }

    "define the unbind" in {
      val test: Call =
        controllers.routes.PreferenceController.preference(CustomsServiceKey, EORINumber, IdentifierValue("123"), Email)
      test.url mustBe "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/email"
    }
  }
}

class PreferenceServiceMock extends PreferenceService(mock[PreferenceResolver]) {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def getChannelPreference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel
  )(implicit
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Either[PreferenceError, JsValue]] =
    Future.successful(
      Json
        .toJson(
          EmailVerification(
            EmailAddress("test@email.com"),
            Instant.parse("2022-03-20T01:01:00Z")
          )
        )
        .asRight
    )
}
