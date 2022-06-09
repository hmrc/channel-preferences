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

import cats.data.{ EitherT, NonEmptySet }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Result }
import uk.gov.hmrc.channelpreferences.controllers.model.{ Consent, ContextualPreference, VerificationId }
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.UnauthorisedPreferenceRequest
import uk.gov.hmrc.channelpreferences.model.preferences._
import uk.gov.hmrc.channelpreferences.services.preferences._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.{ Inject, Singleton }
import scala.collection.immutable.SortedSet
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferenceManagementController @Inject()(
  authorisationEnrolmentService: AuthorisationEnrolmentService,
  preferenceManagementService: PreferenceManagementService,
  controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) {

  def consent(groupId: GroupId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Consent](handleConsent(groupId, _).map(toResult(_, Created)))
  }

  def navigation(groupId: GroupId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Consent](handleConsent(groupId, _).map(toResult(_, Created)))
  }

  private def handleConsent(
    groupId: GroupId,
    consent: Consent
  )(implicit headerCarrier: HeaderCarrier): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      authorizedEnrolments <- authoriseForGroupId(groupId)
      verification         <- EitherT(preferenceManagementService.updateConsent(groupId, consent, authorizedEnrolments))
    } yield verification).value

  def verify(
    groupId: GroupId,
    channel: Channel,
    index: Index
  ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[EmailAddress](
      handleVerification(groupId, channel, index, _).map(toResult(_, Created))
    )(request, implicitly[Manifest[EmailAddress]], EmailAddress.objectFormat)
  }

  private def handleVerification(
    groupId: GroupId,
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress
  )(implicit headerCarrier: HeaderCarrier): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      authorizedEnrolments <- authoriseForGroupId(groupId)
      verification <- EitherT(
                       preferenceManagementService
                         .createVerification(groupId, channel, index, emailAddress, authorizedEnrolments))
    } yield verification).value

  def confirm(verificationId: VerificationId): Action[AnyContent] = Action.async { implicit request =>
    val confirmation = for {
      enrolments <- EitherT(
                     authorisationEnrolmentService
                       .getAuthorisedEnrolments()
                       .map(nonEmptyEnrolments))
      confirmation <- EitherT(preferenceManagementService.confirm(verificationId, enrolments))
    } yield confirmation

    confirmation.value
      .map(toResult(_, Created))
  }

  def getPreference(groupId: GroupId): Action[AnyContent] =
    Action.async { implicit request =>
      handleGetPreference(groupId).map(toResult(_, Ok))
    }

  private def handleGetPreference(groupId: GroupId)(
    implicit headerCarrier: HeaderCarrier): Future[Either[PreferenceError, ContextualPreference]] =
    (for {
      authorizedEnrolments <- authoriseForGroupId(groupId)
      verification         <- EitherT(preferenceManagementService.getPreference(groupId, authorizedEnrolments))
    } yield verification).value

  def toResult(eitherResult: Either[PreferenceError, ContextualPreference], status: Status): Result =
    eitherResult.fold(
      PreferenceError.toResult,
      result => status(Json.toJson(result))
    )

  private def authoriseForGroupId(groupId: GroupId)(
    implicit headerCarrier: HeaderCarrier): EitherT[Future, PreferenceError, NonEmptySet[Enrolment]] = {

    val groupEnrolments = authorisationEnrolmentService
      .getAuthorisedEnrolments()
      .map(Group.findMatchingEnrolments(groupId, _))
      .map(nonEmptyEnrolments)

    EitherT(groupEnrolments)
  }

  private def nonEmptyEnrolments(enrolments: Set[Enrolment]): Either[PreferenceError, NonEmptySet[Enrolment]] =
    NonEmptySet
      .fromSet(SortedSet[Enrolment]() ++ enrolments)
      .toRight(UnauthorisedPreferenceRequest)
}
