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

import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Result }
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ContextualPreference, VerificationId }
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences._
import uk.gov.hmrc.channelpreferences.services.preferences._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferenceManagementController @Inject()(
  preferenceManagementService: PreferenceManagementService,
  controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) {

  def consent(enrolment: Enrolment): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Consent](handleConsent(enrolment, _).map(toResult(_, Created)))
  }

  private def handleConsent(
    enrolment: Enrolment,
    consent: Consent
  ): Future[Either[PreferenceError, ContextualPreference]] =
    preferenceManagementService.updateConsent(enrolment, consent)

  def verify(
    enrolment: Enrolment,
    channel: Channel,
    index: Index
  ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[EmailAddress](
      handleVerification(enrolment, channel, index, _).map(toResult(_, Created))
    )(request, implicitly[Manifest[EmailAddress]], EmailAddress.objectFormat)
  }

  private def handleVerification(
    enrolment: Enrolment,
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress
  ): Future[Either[PreferenceError, ContextualPreference]] =
    preferenceManagementService.createVerification(enrolment, channel, index, emailAddress)

  def confirm(verificationId: VerificationId): Action[AnyContent] = Action.async { _ =>
    preferenceManagementService.confirm(verificationId).map(toResult(_, Created))
  }

  def getPreference(enrolment: Enrolment): Action[AnyContent] =
    Action.async { _ =>
      handleGetPreference(enrolment).map(toResult(_, Ok))
    }

  private def handleGetPreference(enrolment: Enrolment): Future[Either[PreferenceError, ContextualPreference]] =
    preferenceManagementService.getPreference(enrolment)

  def toResult(eitherResult: Either[PreferenceError, ContextualPreference], status: Status): Result =
    eitherResult.fold(
      PreferenceError.toResult,
      result => status(Json.toJson(result))
    )
}
