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

import cats.data.NonEmptySet
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.channelpreferences.controllers.model.{ ContextualPreference, NavigationContext, PreferenceContext }
import uk.gov.hmrc.channelpreferences.model.cds.Email
import uk.gov.hmrc.channelpreferences.model.preferences.{ ChannelledEnrolment, CustomsServiceEnrolment, Enrolment, GroupId, IdentifierValue, PensionsAdministratorEnrolment }
import uk.gov.hmrc.http.HeaderCarrier

class PreferenceManagementServiceSpec extends AnyFlatSpec with Matchers with ScalaFutures {
  behavior of "PreferenceManagementServiceSpec.insertNavigation"
  it should "return ContextualPreference" in new Scope {

    val navigationContext: NavigationContext = NavigationContext(Some(Map("returnUrl" -> "/whatever")))

    val preferenceNavigationContext: ContextualPreference = PreferenceContext(navigationContext)

    PreferenceManagementService
      .insertNavigation(any[GroupId], any[NavigationContext], any[NonEmptySet[Enrolment]])
      .futureValue mustBe Right(preferenceNavigationContext)

  }

  trait Scope {
    val customsDataStorePreferenceProvider: CustomsDataStorePreferenceProvider =
      mock[CustomsDataStorePreferenceProvider]
    val customsServiceEnrolment: CustomsServiceEnrolment = CustomsServiceEnrolment(IdentifierValue("foo"))
    val channelledEnrolment: ChannelledEnrolment = ChannelledEnrolment(customsServiceEnrolment, Email)
    val pensionsAdministratorEnrolment: PensionsAdministratorEnrolment = PensionsAdministratorEnrolment(
      IdentifierValue("bar"))

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val preferenceManagementService: PreferenceManagementService = PreferenceManagementService
  }

}
