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

package uk.gov.hmrc.channelpreferences.services.preferences

import cats.syntax.either._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.IdiomaticMockito.StubbingOps
import play.api.libs.json.JsObject
import uk.gov.hmrc.channelpreferences.model.cds.{ Channel, Email, Phone }
import uk.gov.hmrc.channelpreferences.model.preferences.EnrolmentKey.CustomsServiceKey
import uk.gov.hmrc.channelpreferences.model.preferences.IdentifierKey.EORINumber
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnsupportedChannelError
import uk.gov.hmrc.channelpreferences.model.preferences.{ CustomsServiceEnrolment, EnrolmentKey, IdentifierKey, IdentifierValue }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PreferenceServiceSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  behavior of "PreferenceService.getChannelPreference"

  it should "return a preference for valid inputs" in new Scope {
    preferenceService
      .getChannelPreference(enrolmentKey, identifierKey, identifierValue, channel)
      .futureValue shouldBe JsObject.empty.asRight
  }

  it should "return a preference error provided from resolution" in new Scope {
    private val error = UnsupportedChannelError(Phone)

    preferenceResolver
      .resolveChannelPreference(CustomsServiceEnrolment(identifierValue, channel))
      .returns(Future.successful(error.asLeft))

    preferenceService
      .getChannelPreference(enrolmentKey, identifierKey, identifierValue, channel)
      .futureValue shouldBe error.asLeft
  }

  trait Scope {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val enrolmentKey: EnrolmentKey = CustomsServiceKey
    val identifierKey: IdentifierKey = EORINumber
    val identifierValue: IdentifierValue = IdentifierValue("foo")
    val channel: Channel = Email

    val preferenceResolver: PreferenceResolver = mock[PreferenceResolver]
    preferenceResolver
      .resolveChannelPreference(CustomsServiceEnrolment(identifierValue, channel))
      .returns(Future.successful(JsObject.empty.asRight))

    val preferenceService: PreferenceService = new PreferenceService(preferenceResolver)
  }
}
