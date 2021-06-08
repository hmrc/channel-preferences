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
import uk.gov.hmrc.auth.core.{ AffinityGroup, AuthConnector, AuthorisationException, AuthorisedFunctions, ConfidenceLevel }
import uk.gov.hmrc.channelpreferences.hub.cds.model.Channel
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
@SuppressWarnings(Array("org.wartremover.warts.All"))
@Singleton
class PreferenceController @Inject()(
  cdsPreference: CdsPreference,
  val authConnector: AuthConnector,
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

  def activate(entityId: String, itsaId: String): Action[AnyContent] =
    Action.async {
      if (entityId != "450262a0-1842-4885-8fa1-6fbc2aeb867d") {
        Future.successful(Ok(s"$entityId $itsaId"))
      } else {
        Future.successful(Conflict(s"450262a0-1842-4885-8fa1-6fbc2aeb867d $itsaId"))
      }
    }

  def agentEnrolment(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[AgentEnrolment] { enrolment =>
      authorised(AffinityGroup.Agent and ConfidenceLevel.L200)
        .retrieve(Retrievals.affinityGroup) { _ =>
          Future.successful(
            Ok(s"Agent Enrolment Successful for ARN:'${enrolment.arn}'and itsaId '${enrolment.itsaId}''"))
        }
        .recoverWith {
          case e: AuthorisationException => Future.successful(Unauthorized(e.getMessage))
        }
    }
  }
}
