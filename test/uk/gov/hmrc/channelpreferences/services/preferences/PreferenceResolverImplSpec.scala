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
import org.mockito.IdiomaticMockito.StubbingOps
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.JsObject
import uk.gov.hmrc.channelpreferences.model.cds.{ Email, Phone }
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnsupportedChannelError
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelledEnrolment, CustomsServiceEnrolment, IdentifierValue }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class PreferenceResolverImplSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  behavior of "PreferenceResolverImpl.resolvePreferenceForEnrolment"

  it should "resolve a preference for a customs enrolment" in new Scope {
    preferenceResolverImpl
      .resolveChannelPreference(channelledEnrolment)
      .futureValue shouldBe JsObject.empty.asRight
  }

  it should "return a preferences error from a provider" in new Scope {
    private val error = UnsupportedChannelError(Phone)
    customsDataStorePreferenceProvider.getChannelPreference(customsServiceEnrolment, Phone) returns Future.successful(
      error.asLeft)

    preferenceResolverImpl
      .resolveChannelPreference(channelledEnrolment.copy(channel = Phone))
      .futureValue shouldBe error.asLeft
  }

  trait Scope {
    val customsDataStorePreferenceProvider: CustomsDataStorePreferenceProvider =
      mock[CustomsDataStorePreferenceProvider]
    val customsServiceEnrolment: CustomsServiceEnrolment = CustomsServiceEnrolment(IdentifierValue("foo"))
    val channelledEnrolment: ChannelledEnrolment = ChannelledEnrolment(customsServiceEnrolment, Email)
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    customsDataStorePreferenceProvider.getChannelPreference(customsServiceEnrolment, Email) returns Future.successful(
      JsObject.empty.asRight)

    val preferenceResolverImpl: PreferenceResolverImpl = new PreferenceResolverImpl(
      customsDataStorePreferenceProvider
    )
  }
}
