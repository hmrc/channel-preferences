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
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, AuthorisedFunctions }
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.channelpreferences.hub.cds.model.Channel
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
@Singleton
class PreferenceController @Inject()(
  cdsPreference: CdsPreference,
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
      // (1) Ask the entity-resolver service to lookup the given ITSA enrolled entityId
      entityResolverConnector
        .resolveBy(enrolment.entityId)
        .map { entity =>
          assert(enrolment.entityId == entity.id)
          (entity.saUtr, entity.nino /*, entity.itsa */ )

          // TODO Implement the business logic designed by Micheal in place of the following example provided by Satya
          if (enrolment.entityId != "450262a0-1842-4885-8fa1-6fbc2aeb867d") {
            Ok(s"$enrolment")
          } else {
            Conflict(s"450262a0-1842-4885-8fa1-6fbc2aeb867d ${enrolment.itsaId}")
          }
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
}
