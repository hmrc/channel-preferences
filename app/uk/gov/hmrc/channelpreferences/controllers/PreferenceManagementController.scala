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

import cats.data.EitherT
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ContextualPreference, Verification, VerificationId }
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences._
import uk.gov.hmrc.channelpreferences.services.PreferenceManagementService
import uk.gov.hmrc.channelpreferences.services.preferences.PreferenceResolver
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceManagementController @Inject()(
  preferenceManagementService: PreferenceManagementService,
  controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) {

  def consent(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue
  ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Consent](handleConsent(enrolmentKey, identifierKey, identifierValue, _).map(toResult))
  }

  private def handleConsent(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    consent: Consent
  ): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      enrolment <- EitherT.fromEither[Future](
                    PreferenceResolver.toEnrolment(enrolmentKey, identifierKey, identifierValue))
      preference <- EitherT(preferenceManagementService.updateConsent(enrolment, consent))
    } yield preference).value

  def verify(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel,
    index: Index
  ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Verification](
      handleVerification(enrolmentKey, identifierKey, identifierValue, channel, index, _).map(toResult))
  }

  private def handleVerification(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue,
    channel: Channel,
    index: Index,
    verification: Verification
  ): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      channelledEnrolment <- EitherT.fromEither[Future](
                              PreferenceResolver
                                .toChannelledEnrolment(enrolmentKey, identifierKey, identifierValue, channel))
      preference <- EitherT(PreferenceManagementService.createVerification(channelledEnrolment, index, verification))
    } yield preference).value

  def confirm(
    verificationId: VerificationId
  ): Action[AnyContent] = Action.async { _ =>
    PreferenceManagementService.confirm(verificationId).map(toResult)
  }

  def getPreference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue): Action[AnyContent] =
    Action.async { _ =>
      handleGetPreference(enrolmentKey, identifierKey, identifierValue).map(toResult)
    }

  private def handleGetPreference(
    enrolmentKey: EnrolmentKey,
    identifierKey: IdentifierKey,
    identifierValue: IdentifierValue): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      enrolment <- EitherT.fromEither[Future](
                    PreferenceResolver.toEnrolment(enrolmentKey, identifierKey, identifierValue))
      preference <- EitherT(PreferenceManagementService.getPreference(enrolment))
    } yield preference).value
}
