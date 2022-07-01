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

import cats.syntax.either._
import org.joda.time.DateTime
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Call
import play.api.test.Helpers.{ route, _ }
import play.api.test._
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, EmailVerification }
import uk.gov.hmrc.channelpreferences.model.preferences.EnrolmentKey.CustomsServiceKey
import uk.gov.hmrc.channelpreferences.model.preferences.IdentifierKey.EORINumber
import uk.gov.hmrc.channelpreferences.model.preferences.{ EnrolmentKey, IdentifierKey, IdentifierValue, PreferenceError }
import uk.gov.hmrc.channelpreferences.services.preferences.{ PreferenceResolver, PreferenceService }
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.{ ExecutionContext, Future }

class ChannelBinderSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "ChannelBinder" must {
    "define the bind - success" in new Scope {
      private val request = FakeRequest(
        GET,
        "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/email")
      private val test = route(appBuilder, request).get
      status(test) mustBe OK
    }

    "define the bind - failure" in new Scope {
      private val request = FakeRequest(
        GET,
        "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/badChannel")
      private val test = route(appBuilder, request).get
      status(test) mustBe BAD_REQUEST
    }

    "define the unbind" in new Scope {
      val test: Call =
        controllers.routes.PreferenceController.preference(CustomsServiceKey, EORINumber, IdentifierValue("123"), Email)
      test.url mustBe "/channel-preferences/preferences/enrolments/HMRC-CUS-ORG/identifier-keys/EORINumber/identifier-values/123/channels/email"
    }
  }
}

class PreferenceServiceMock extends PreferenceService(mock[PreferenceResolver]) {
  override def getChannelPreference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel)(
    implicit headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext): Future[Either[PreferenceError, JsValue]] =
    Future.successful(
      Json
        .toJson(
          EmailVerification(
            EmailAddress("test@email.com"),
            new DateTime(2022, 3, 20, 1, 1)
          ))
        .asRight
    )
}

trait Scope {
  val appBuilder: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false)
    .overrides(bind[PreferenceService].to[PreferenceServiceMock])
    .build()
}
