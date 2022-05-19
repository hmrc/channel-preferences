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

package uk.gov.hmrc.channelpreferences.controllers

import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Headers, Result }
import play.api.test.Helpers.{ contentAsJson, defaultAwaitTimeout, status }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.channelpreferences.controllers.model._
import uk.gov.hmrc.channelpreferences.model.cds.Email
import uk.gov.hmrc.channelpreferences.model.preferences.{ Enrolment, PrimaryIndex }
import uk.gov.hmrc.channelpreferences.services.preferences.PreferenceManagementService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class PreferenceManagementControllerSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "PreferenceManagementController.consent"

  it should "return a preference context when successfully updating consent" in new Scope {
    preferenceManagementService.updateConsent(enrolment, consent) returns Future.successful(
      Right(contextualPreferenceConsent))

    val payload: JsValue = Json.parse(readContextResource("consent.json"))
    val result: Future[Result] =
      preferenceManagementController
        .consent(enrolment)
        .apply(FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), payload))

    status(result) mustBe Status.CREATED
    contentAsJson(result).validate[ContextualPreference].get mustBe contextualPreferenceConsent
  }

  it should "return a preference context when successfully creating a verification" in new Scope {

    preferenceManagementService
      .createVerification(enrolment, Email, PrimaryIndex, emailAddress) returns Future
      .successful(Right(contextualPreferenceVerification))

    val payload: JsValue = Json.parse(s"""{ "value": "${emailAddress.value}" }""")
    val result: Future[Result] =
      preferenceManagementController
        .verify(enrolment, Email, PrimaryIndex)
        .apply(FakeRequest("POST", "", Headers("Content-Type" -> "application/json"), payload))

    status(result) mustBe Status.CREATED
    contentAsJson(result).validate[ContextualPreference].get mustBe contextualPreferenceVerification
  }

  it should "return a preference context when successfully confirming a verification" in new Scope {
    preferenceManagementService.confirm(verificationId) returns Future.successful(Right(preferenceWithoutContext))

    val result: Future[Result] =
      preferenceManagementController
        .confirm(verificationId)
        .apply(FakeRequest("POST", ""))

    status(result) mustBe Status.CREATED
    contentAsJson(result).validate[ContextualPreference].get mustBe preferenceWithoutContext
  }

  it should "return a preference context when successfully getting a preference" in new Scope {
    preferenceManagementService.getPreference(enrolment) returns Future.successful(Right(preferenceWithContext))

    val result: Future[Result] =
      preferenceManagementController
        .getPreference(enrolment)
        .apply(FakeRequest())

    status(result) mustBe Status.OK
    contentAsJson(result).validate[ContextualPreference].get mustBe preferenceWithContext
  }

  trait Scope extends TestModels {
    val enrolment: Enrolment = Enrolment.fromValue(enrolmentValue).right.value
    val contextualPreferenceConsent: ContextualPreference = PreferenceContext(consent)
    val verificationConsent: ConsentVerificationContext = ConsentVerificationContext(
      consent,
      verification
    )
    val contextualPreferenceVerification: ContextualPreference = PreferenceContext(verificationConsent)
    val preferenceWithoutContext: ContextualPreference = PreferenceWithoutContext(preference)
    val verificationContext: Context = VerificationContext(verification)
    val preferenceWithContext: ContextualPreference = PreferenceWithContext(preference, List(verificationContext))

    val preferenceManagementService: PreferenceManagementService = mock[PreferenceManagementService]
    val preferenceManagementController = new PreferenceManagementController(
      preferenceManagementService,
      Helpers.stubControllerComponents()
    )

    def readContextResource(fileName: String): String = readResource("context", fileName)

    private def readResource(folder: String, fileName: String): String = {
      val resource = Source.fromURL(getClass.getResource(s"/$folder/$fileName"))
      val str = resource.mkString
      resource.close()
      str
    }
  }
}
