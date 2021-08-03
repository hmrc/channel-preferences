/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, AuthorisedFunctions }
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.channelpreferences.hub.cds.model.Channel
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
@Singleton
class PreferenceController @Inject()(
  val cdsPreference: CdsPreference,
  val authConnector: AuthConnector,
  val entityResolverConnector: EntityResolverConnector,
  override val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions {

  implicit val emailWrites = uk.gov.hmrc.channelpreferences.hub.cds.model.EmailVerification.emailVerificationFormat
  def preference(channel: Channel, enrolmentKey: String, taxIdName: String, taxIdValue: String): Action[AnyContent] =
    Action.async { implicit request =>
      cdsPreference.getPreference(channel, enrolmentKey, taxIdName, taxIdValue).map {
        case Right(e)              => Ok(Json.toJson(e))
        case Left(NOT_FOUND)       => NotFound
        case Left(NOT_IMPLEMENTED) => NotImplemented
        case Left(_)               => BadGateway
      }
    }

  def confirm(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Enrolment] { enrolment =>
      authorised( /* TODO Do we need to pass any affinity group here? */ )
        .retrieve(Retrievals.saUtr) {
          case authTokenSaUtr =>
            doConfirmItsa(enrolment.entityId, enrolment.itsaId, authTokenSaUtr)
        }
        .recoverWith {
          case e: AuthorisationException =>
            reply(UNAUTHORIZED, e.getMessage)
        }
    }
  }

  def enrolment(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[AgentEnrolment] { enrolment =>
      authorised(Agent)
        .retrieve(Retrievals.affinityGroup) { _ =>
          Future.successful(Ok(s"Enrolment Successful for arn: '${enrolment.arn}', itsaId :'${enrolment.itsaId}'" +
            s", nino: '${enrolment.nino}', sautr: '${enrolment.sautr}'"))
        }
        .recoverWith {
          case e: AuthorisationException => Future.successful(Unauthorized(e.getMessage))
        }
    }
  }

  def processBounce(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Event] { event =>
      Future.successful(
        Created(s"Bounce sucessfully processed: '${event.eventId}', subject :'${event.subject}'" +
          s", groupId: '${event.groupId}', timeStamp: '${event.timeStamp}', event: '${event.event}'"))
    }
  }

  // ----------------------
  // See the case definition on https://jira.tools.tax.service.gov.uk/browse/DC-3504
  private def doConfirmItsa(passedBackEntityId: String, passedBackItsaId: String, authTokenSaUtr: Option[String])(
    implicit hc: HeaderCarrier): Future[Result] =
    entityResolverConnector
      .resolveBy(passedBackEntityId)
      .flatMap { maybeEntityById =>
        maybeEntityById.fold(reply(NOT_FOUND, "Invalid entity id or entity id has expired")) { entityById =>
          (entityById.itsa, entityById.saUtr, authTokenSaUtr) match {
            // case 1.5
            case (Some(entityItsa), _, _) if entityItsa != passedBackItsaId =>
              reply(UNAUTHORIZED, "entityId already has a different itsaId linked to it in entity resolver")
            // case 1.2
            case (_, Some(entitySaUtr), Some(authSaUtr)) if entitySaUtr != authSaUtr =>
              reply(UNAUTHORIZED, "SAUTR in Auth token is different from SAUTR in entity resolver")

            case (_, _, Some(authSaUtr)) =>
              entityResolverConnector.resolveBySaUtr(authSaUtr).flatMap {

                // case 1.3
                case Some(entityBySaUtr) if entityBySaUtr._id != passedBackEntityId =>
                  reply(UNAUTHORIZED, "itsaId is already linked to a different entityid in entity resolver")

                case _ =>
                  finalCheck(authTokenSaUtr, entityById, passedBackEntityId, passedBackItsaId)
              }
            case _ =>
              finalCheck(authTokenSaUtr, entityById, passedBackEntityId, passedBackItsaId)
          }
        }
      }

  private def finalCheck(
    maybeAuthSaUtr: Option[String],
    entityById: Entity,
    passedBackEntityId: String,
    passedBackItsaId: String)(implicit hc: HeaderCarrier): Future[Result] =
    entityResolverConnector.resolveByItsa(passedBackItsaId).flatMap { maybeEntityByItsa =>
      (maybeEntityByItsa.map(_._id), maybeAuthSaUtr, entityById.saUtr) match {

        case (Some(entityId), _, _) if entityId != passedBackEntityId =>
          // case 1.6
          reply(UNAUTHORIZED, "itsaId is already linked to a different entityid in entity resolver")

        // case 1.4
        case (_, None, _) =>
          entityResolverConnector
            .update(entityById.copy(itsa = Some(passedBackItsaId)))
            .flatMap { _ =>
              reply(OK, "itsaId successfully linked to entityId")
            }

        // case 1.1
        case (_, Some(authSaUtr), Some(entitySaUtr)) if authSaUtr == entitySaUtr =>
          entityResolverConnector
            .update(entityById)
            .flatMap { _ =>
              reply(OK, "itsaId successfully linked to entityId")
            }

        case _ =>
          // shouldn't happen
          reply(INTERNAL_SERVER_ERROR, "Unexpected setting")
      }
    }

  private def reply(code: Int, reason: String): Future[Result] =
    Future.successful(Status(code).apply(Json.obj("reason" -> reason)))
}
