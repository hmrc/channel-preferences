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

import cats.data.{ EitherT, NonEmptyList, NonEmptySet }
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.option._
import com.google.inject.Inject
import uk.gov.hmrc.channelpreferences.controllers.model.{ ConsentVerificationContext, _ }
import uk.gov.hmrc.channelpreferences.model.cds.Channel
import uk.gov.hmrc.channelpreferences.model.preferences.PreferenceError.StateTransitionError
import uk.gov.hmrc.channelpreferences.model.preferences._
import uk.gov.hmrc.channelpreferences.repository.{ ContextRepository, PreferenceRepository }
import uk.gov.hmrc.channelpreferences.utils.{ Clock, Random }

import javax.inject.Singleton
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferenceManagementServiceImpl @Inject()(
  contextRepository: ContextRepository,
  preferenceRepository: PreferenceRepository,
  clock: Clock,
  random: Random
)(implicit executionContext: ExecutionContext)
    extends PreferenceManagementService {

  override def getPreference(enrolments: NonEmptySet[Enrolment]): Future[Option[ContextualPreference]] =
    getContextAndPreference(enrolments).map {
      case (Some(contextPayload), Some(preference)) => PreferenceWithContext(preference, contextPayload.context).some
      case (Some(contextPayload), None)             => PreferenceContext(contextPayload.context).some
      case (None, Some(preference))                 => PreferenceWithoutContext(preference).some
      case _                                        => none
    }

  override def updateConsent(
    consent: Consent,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, ContextualPreference]] =
    getContextAndPreference(enrolments).flatMap {
      case (Some(contextPayload), Some(preference)) =>
        val updated = preference.copy(consents = NonEmptyList.of(consent))
        preferenceRepository
          .update(updated)
          .map(_ => PreferenceWithContext(updated, contextPayload.context).asRight[PreferenceError])

      case (None, Some(preference)) =>
        val updated = preference.copy(consents = NonEmptyList.of(consent))
        preferenceRepository
          .update(preference)
          .map(_ => PreferenceWithoutContext(updated).asRight[PreferenceError])

      case (Some(contextPayload), None) =>
        val updated = contextPayload.context.updateConsent(consent)
        contextRepository
          .updateContext(contextPayload.copy(context = updated))
          .map(_ => PreferenceContext(updated).asRight[PreferenceError])

      case _ =>
        val context = ConsentContext(consent, none)
        contextRepository
          .addContext(ContextPayload(EnrolmentContextId(enrolments.toNonEmptyList), clock.currentDateTime, context))
          .map(_ => PreferenceContext(context).asRight[PreferenceError])
    }

  override def updateNavigation(
    navigationContext: NavigationContext,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, NavigationContext]] =
    contextRepository
      .findContext(enrolments.toNonEmptyList)
      .flatMap {
        case Some(value) =>
          val updated =
            value.copy(context = value.context.updateNavigation(navigationContext.navigation.getOrElse(Map.empty)))
          contextRepository
            .updateContext(updated)
            .map(_ => NavigationContext(updated.context.navigation).asRight[PreferenceError])

        case None =>
          contextRepository
            .addContext(
              ContextPayload(EnrolmentContextId(enrolments.toNonEmptyList), clock.currentDateTime, navigationContext))
            .map(_ => navigationContext.asRight)
      }

  override def createVerification(
    channel: Channel,
    index: Index,
    emailAddress: EmailAddress,
    enrolments: NonEmptySet[Enrolment]
  ): Future[Either[PreferenceError, ContextualPreference]] = {
    val verification = Verification(VerificationId(random), emailAddress, clock.currentDateTime)

    contextRepository
      .findContext(enrolments.toNonEmptyList)
      .flatMap {
        case Some(contextPayload) =>
          val updated = contextPayload.copy(context = contextPayload.context.updateVerification(verification))
          contextRepository
            .updateContext(updated)
            .map(_ => PreferenceContext(updated.context).asRight)

        case _ =>
          val verificationContext = VerificationContext(verification, None)
          contextRepository
            .addContext(
              ContextPayload(
                IndexedEnrolmentContextId(enrolments.toNonEmptyList, channel, index),
                clock.currentDateTime,
                verificationContext
              )
            )
            .map(_ => PreferenceContext(verificationContext).asRight)
      }
  }

  override def confirm(verificationId: VerificationId): Future[Either[PreferenceError, Option[ContextualPreference]]] =
    contextRepository
      .findVerification(verificationId)
      .flatMap {
        case Some(contextPayload) =>
          contextPayload.context match {
            case _: ConfirmationContext =>
              Future.successful(StateTransitionError(
                s"The verification, ${verificationId.id.toString}, has already been confirmed.").asLeft)

            case v: VerificationContext =>
              confirm(v.verification, contextPayload.contextId)
                .map(_.map(PreferenceWithoutContext(_)))
                .value

            case c: ConsentVerificationContext =>
              confirm(c.verification, contextPayload.contextId).flatMap {
                case Some(preference) =>
                  EitherT.fromEither[Future](PreferenceWithoutContext(preference).some.asRight[PreferenceError])
                case None =>
                  confirmPreferenceCreation(contextPayload.contextId, c).map(PreferenceWithoutContext(_).some)
              }.value

            case _ => Future.successful(StateTransitionError("Cannot confirm a context with no verification").asLeft)
          }
        case _ => Future.successful(none.asRight)
      }

  private def confirm(
    verification: Verification,
    contextId: ContextId): EitherT[Future, PreferenceError, Option[Preference]] =
    EitherT(
      preferenceRepository
        .get(contextId.enrolments)
        .map {
          case Some(preference) => preference.confirm(verification).map(_.some)
          case None             => none.asRight
        })

  private def confirmPreferenceCreation(
    contextId: ContextId,
    consentVerificationContext: ConsentVerificationContext): EitherT[Future, PreferenceError, Preference] = {
    val preference = Preference.defaultConfirmedPreference(contextId, consentVerificationContext, clock)
    for {
      _ <- EitherT(preferenceRepository.insert(preference))
      _ <- EitherT(contextRepository.deleteContext(contextId.enrolments).map(_.asRight[PreferenceError]))
    } yield preference
  }

  private def getContextAndPreference(
    enrolments: NonEmptySet[Enrolment]): Future[(Option[ContextPayload], Option[Preference])] = {
    val enrolmentsList = enrolments.toNonEmptyList
    (contextRepository.findContext(enrolmentsList), preferenceRepository.get(enrolmentsList)).tupled
  }
}
