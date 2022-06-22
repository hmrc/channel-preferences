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
import play.api.libs.json.JsObject
import uk.gov.hmrc.channelpreferences.controllers.model.{ ConsentVerificationContext, PreferenceContext }
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
        .withHttpHeaders(jsonHeader, setup.authHeader)
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
        .withHttpHeaders(jsonHeader)
        .put(consentJson)
        .futureValue

    response.status mustBe Status.UNAUTHORIZED
  }

  s"PUT to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent endpoint" should
    "fail without the correct authentication" in scope(List(PensionsPractitionerEnrolment(IdentifierValue("foo")))) {
    setup =>
      val response =
        setup.wsClient
          .url(
            setup.resource(s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
          .withHttpHeaders(jsonHeader, setup.authHeader)
          .put(consentJson)
          .futureValue

      response.status mustBe Status.UNAUTHORIZED
  }

  s"POST to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify" should
    "pass with a valid payload and authentication" in scope(enrolments.toList) { setup =>
    setup.wsClient
      .url(setup.resource(s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
      .withHttpHeaders(jsonHeader, setup.authHeader)
      .put(consentJson)
      .futureValue
      .status mustBe Status.CREATED

    val response =
      setup.wsClient
        .url(setup.resource(
          s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify"))
        .withHttpHeaders(jsonHeader, setup.authHeader)
        .post(emailJson)
        .futureValue

    response.status mustBe Status.CREATED
  }

  s"POST to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify" should
    "fail without authentication" in scope(enrolments.toList) { setup =>
    val response =
      setup.wsClient
        .url(setup.resource(
          s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify"))
        .withHttpHeaders(jsonHeader)
        .post(emailJson)
        .futureValue

    response.status mustBe Status.UNAUTHORIZED
  }

  s"POST to /channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify" should
    "fail without the correct authentication" in scope(List(PensionsPractitionerEnrolment(IdentifierValue("foo")))) {
    setup =>
      val response =
        setup.wsClient
          .url(setup.resource(
            s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify"))
          .withHttpHeaders(jsonHeader, setup.authHeader)
          .post(emailJson)
          .futureValue

      response.status mustBe Status.UNAUTHORIZED
  }

  s"PUT to /channel-preferences/preferences/verify/${verificationId.id}/confirm" should
    "pass with authentication" in scope(enrolments.toList) { setup =>
    setup.wsClient
      .url(setup.resource(s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/consent"))
      .withHttpHeaders(jsonHeader, setup.authHeader)
      .put(consentJson)
      .futureValue
      .status mustBe Status.CREATED

    val verificationResult = setup.wsClient
      .url(setup.resource(
        s"/channel-preferences/preferences/enrolments/${PensionsAdministratorGroupId.name}/channels/email/index/primary/verify"))
      .withHttpHeaders(jsonHeader, setup.authHeader)
      .post(emailJson)
      .futureValue

    verificationResult.status mustBe Status.CREATED
    val preferenceContext = verificationResult.json.as[PreferenceContext]

    val responseId = preferenceContext.context match {
      case ConsentVerificationContext(_, verification, _) => verification.id
      case other                                          => fail(s"expected a consent verification context, but got $other")
    }

    val response =
      setup.wsClient
        .url(setup.resource(s"/channel-preferences/preferences/verify/${responseId.id.toString}/confirm"))
        .withHttpHeaders(jsonHeader, setup.authHeader)
        .put(JsObject.empty)
        .futureValue

    response.status mustBe Status.CREATED
  }
}
