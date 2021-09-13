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

import play.api.Logger
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Result }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, AuthorisedFunctions }
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.channelpreferences.connectors.EntityResolverConnector
import uk.gov.hmrc.channelpreferences.hub.cds.model.Channel
import uk.gov.hmrc.channelpreferences.hub.cds.services.CdsPreference
import uk.gov.hmrc.channelpreferences.model._
import uk.gov.hmrc.channelpreferences.preferences.model.Event
import uk.gov.hmrc.channelpreferences.preferences.services.ProcessEmail

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class PreferenceController @Inject()(
  cdsPreference: CdsPreference,
  val authConnector: AuthConnector,
  entityResolverConnector: EntityResolverConnector,
  processEmail: ProcessEmail,
  override val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(controllerComponents) with AuthorisedFunctions {

  val logger: Logger = Logger(this.getClass())

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
      entityResolverConnector.confirm(enrolment.entityId, enrolment.itsaId).map { resp =>
        Status(resp.status)(resp.json)
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
      processEmail
        .process(event)
        .map {
          case Right(content) => Ok(content)
          case Left(error) => {
            logger.error(s"Failed to update email bounce $error")
            NotModified
          }
        }
        .recover {
          case error =>
            logger.error(s"Failed to update email bounce ${error.getMessage}")
            InternalServerError
        }
    }
  }

  def update(key: String, status: String): Action[AnyContent] =
    Action.async { _ =>
      key match {
        case "itsa"        => validateItsaStatus(status)
        case unexpectedKey => Future.successful(BadRequest(s"The key $unexpectedKey is not supported"))
      }
    }

  private def handleItsa(status: Boolean): Future[Result] =
    Future.successful(Ok(status.toString)) // TODO use EIS connector

  private def validateItsaStatus(status: String): Future[Result] =
    status.toLowerCase match {
      case "true"           => handleItsa(true)
      case "false"          => handleItsa(false)
      case unexpectedStatus => Future.successful(BadRequest(s"Unexpected status for itsa $unexpectedStatus"))
    }

}
