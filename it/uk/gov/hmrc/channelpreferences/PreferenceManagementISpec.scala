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

import org.scalatest.flatspec.AnyFlatSpec
import play.api.http.Status
import uk.gov.hmrc.channelpreferences.model.preferences.{ IdentifierValue, PensionsAdministratorGroupId, PensionsPractitionerEnrolment }
import uk.gov.hmrc.channelpreferences.util.BaseISpec
import uk.gov.hmrc.channelpreferences.util.Setup.scope

class PreferenceManagementISpec extends AnyFlatSpec with BaseISpec with TestModels {

  behavior of "ContextApi"

  s"PUT to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent endpoint" should "work with valid payload" in scope(
    enrolments.toList) { setup =>
    val response =
      setup.wsClient
        .url(
          setup.resource(s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
        .withHttpHeaders(("Content-Type" -> "application/json"), setup.authHeader)
        .put(consentJson)
        .futureValue

    response.status mustBe Status.CREATED
  }

  s"PUT to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent endpoint" should "fail without authentication" in scope(
    enrolments.toList) { setup =>
    val response =
      setup.wsClient
        .url(
          setup.resource(s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
        .withHttpHeaders(("Content-Type" -> "application/json"))
        .put(consentJson)
        .futureValue

    response.status mustBe Status.UNAUTHORIZED
  }

  s"PUT to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent endpoint" should
    "fail without the correct authentication" in scope(List(PensionsPractitionerEnrolment(IdentifierValue("foo")))) {
    setup =>
      val response =
        setup.wsClient
          .url(setup.resource(
            s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
          .withHttpHeaders(("Content-Type" -> "application/json"), setup.authHeader)
          .put(consentJson)
          .futureValue

      response.status mustBe Status.UNAUTHORIZED
  }
}
